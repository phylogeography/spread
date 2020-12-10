(ns worker.listener
  (:require
   [clojure.data.json :as json]
   [aws.sqs :as aws-sqs]
   [api.db :as db]
   [aws.s3 :as aws-s3]
   [api.models.continuous-tree :as continuous-tree-model]
   [mount.core :as mount :refer [defstate]]
   [shared.utils :refer [file-exists?]]
   [taoensso.timbre :as log])
  (:import (com.spread.parsers ContinuousTreeParser)))

(defonce tmp-dir "/tmp")

(defmulti handler
  (fn [{:message/keys [type]} _]
    type))

(defmethod handler :default
  [{:message/keys [type]} _]
  (log/warn (str "No handler for message type " type)))

(defmethod handler :continuous-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling continuous-tree-upload" args)
  (try
    (let [;; NOTE: make sure UI uploads always with the same extension
          tree-object-key (str user-id "/" id ".tree")
          tree-file-path (str tmp-dir "/" tree-object-key)
          _ (aws-s3/download-file s3 {:bucket bucket-name
                                      :key tree-object-key
                                      :dest-path tree-file-path})
          parser (doto (new ContinuousTreeParser)
                   (.setTreeFilePath tree-file-path))
          [attributes hpd-levels] (json/read-str (.parseAttributesAndHpdLevels parser))]
      (log/info "Parsed attributes and hpd-levels" {:id id
                                                    :attributes attributes
                                                    :hpd-levels hpd-levels})
      (continuous-tree-model/insert-attributes! db id attributes)
      (continuous-tree-model/insert-hpd-levels! db id hpd-levels)
      (continuous-tree-model/update-tree! db {:id id
                                              :status :ATTRIBUTES_AND_HPD_LEVELS_PARSED}))
    (catch Exception e
      (log/error "Exception when handling continuous-tree-upload" {:error e})
      (continuous-tree-model/update-tree! db {:id id
                                              :status :ERROR}))))

(defmethod handler :parse-continuous-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-continuous-tree" args)
  (try
    (let [_ (continuous-tree-model/update-tree! db {:id id
                                                    :status :RUNNING})
          {:keys [user-id x-coordinate-attribute-name y-coordinate-attribute-name
                  hpd-level has-external-annotations timescale-multiplier
                  most-recent-sampling-date]
           :as tree}
          (continuous-tree-model/get-tree db {:id id})
          tree-object-key (str user-id "/" id ".tree")
          tree-file-path (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? tree-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key tree-object-key
                                        :dest-path tree-file-path}))
          ;; call all setters
          parser (doto (new ContinuousTreeParser)
                   (.setTreeFilePath tree-file-path)
                   (.setXCoordinateAttributeName x-coordinate-attribute-name)
                   (.setYCoordinateAttributeName y-coordinate-attribute-name)
                   (.setHpdLevel hpd-level)
                   (.hasExternalAnnotations has-external-annotations)
                   (.setTimescaleMultiplier timescale-multiplier)
                   (.setMostRecentSamplingDate most-recent-sampling-date))
          output-object-key (str user-id "/" id ".json")
          output-object-path (str tmp-dir "/" output-object-key)
          _ (spit output-object-path (.parse parser) :append false)
          _ (aws-s3/upload-file s3 {:bucket bucket-name
                                    :key output-object-key
                                    :file-path output-object-path})
          url (aws-s3/build-url aws-config bucket-name output-object-key)]
      (continuous-tree-model/update-tree! db {:id id
                                              :output-file-url url
                                              :status :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-continuous-tree" {:error e})
      (continuous-tree-model/update-tree! db {:id id
                                              :status :ERROR}))))

(defn start [{:keys [aws db] :as config}]
  (let [{:keys [workers-queue-url bucket-name]} aws
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        db (db/init db)
        context {:s3 s3
                 :db db
                 :bucket-name bucket-name
                 :aws-config aws}]
    (log/info "Starting worker listener" config)
    (loop []
      (try
        ;; If the queue is empty, wait for 2 seconds and poll again
        ;; (log/debug "Polling...")
        (if-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs workers-queue-url)]
          (do
            (handler body context)
            (aws-sqs/ack-message sqs workers-queue-url receipt-handle)))
        (Thread/sleep 2000)
        (catch Exception e
          (log/error "Error processing a message" {:error e})))
      (recur))))

(defstate listener
  :start (start (mount/args)))
