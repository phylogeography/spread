(ns worker.listener
  (:require [api.db :as db]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [clojure.data.json :as json]
            [mount.core :as mount :refer [defstate]]
            [shared.utils :refer [file-exists?]]
            [taoensso.timbre :as log])
  (:import (com.spread.parsers ContinuousTreeParser)
           (com.spread.parsers DiscreteTreeParser)))

(defonce tmp-dir "/tmp")

(defmulti handler
  (fn [{:message/keys [type]} _]
    type))

(defmethod handler :default
  [{:message/keys [type]} _]
  (log/warn (str "No handler for message type " type)))

(defmethod handler :discrete-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling discrete-tree-upload" args)
  (try
    (let [;; TODO: query tree-file and parse key
          tree-object-key (str user-id "/" id ".tree")
          tree-file-path (str tmp-dir "/" tree-object-key)
          _ (aws-s3/download-file s3 {:bucket bucket-name
                                      :key tree-object-key
                                      :dest-path tree-file-path})
          parser (doto (new DiscreteTreeParser)
                   (.setTreeFilePath tree-file-path))
          attributes (json/read-str (.parseAttributes parser))]
      (log/info "Parsed attributes" {:id id
                                     :attributes attributes})
      (discrete-tree-model/insert-attributes! db id attributes)
      (discrete-tree-model/update! db {:id id
                                       :status :ATTRIBUTES_PARSED}))
    (catch Exception e
      (log/error "Exception when handling discrete-tree-upload" {:error e})
      (discrete-tree-model/update! db {:id id
                                       :status :ERROR}))))

(defmethod handler :parse-discrete-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-discrete-tree" args)
  (try
    (let [_ (discrete-tree-model/update! db {:id id
                                             :status :RUNNING})
          {:keys [user-id location-attribute-name
                  timescale-multiplier most-recent-sampling-date
                  tree-file-url locations-file-url] :as tree}
          (discrete-tree-model/get-tree db {:id id})

          tree-object-key (str user-id "/" id ".tree")
          tree-file-path (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? tree-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key tree-object-key
                                        :dest-path tree-file-path}))

          locations-file-id (s3-url->id locations-file-url bucket-name user-id)
          locations-object-key (str user-id "/" locations-file-id ".txt")
          locations-file-path (str tmp-dir "/" locations-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? locations-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key locations-object-key
                                        :dest-path locations-file-path}))

          ;; call all setters
          parser (doto (new DiscreteTreeParser)
                   (.setTreeFilePath tree-file-path)
                   (.setLocationsFilePath locations-file-path)
                   (.setLocationTraitAttributeName location-attribute-name)
                   (.setTimescaleMultiplier timescale-multiplier)
                   (.setMostRecentSamplingDate most-recent-sampling-date))

          output-object-key (str user-id "/" id ".json")
          output-object-path (str tmp-dir "/" output-object-key)
          _ (spit output-object-path (.parse parser) :append false)
          _ (aws-s3/upload-file s3 {:bucket bucket-name
                                    :key output-object-key
                                    :file-path output-object-path})
          url (aws-s3/build-url aws-config bucket-name output-object-key)]
      (discrete-tree-model/update! db {:id id
                                       :output-file-url url
                                       :status :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-discrete-tree" {:error e})
      (discrete-tree-model/update! db {:id id
                                       :status :ERROR}))))

(defmethod handler :continuous-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling continuous-tree-upload" args)
  (try
    (let [;; TODO: query tree-file and parse key
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
                  most-recent-sampling-date]}
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
        (when-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs workers-queue-url)]
          (handler body context)
          (aws-sqs/ack-message sqs workers-queue-url receipt-handle))
        (Thread/sleep 2000)
        (catch Exception e
          (log/error "Error processing a message" {:error e})))
      (recur))))

(defstate listener
  :start (start (mount/args)))
