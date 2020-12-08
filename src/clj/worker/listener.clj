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
          _ (aws-s3/download-file s3 bucket-name tree-object-key tree-file-path)
          parser (doto (new ContinuousTreeParser)
                   (.setTreeFilePath tree-file-path))
          [attributes hpd-levels] (json/read-str (.parseAttributesAndHpdLevels parser))]
      (log/info "Parsed attributes and hpd-levels" {:id id
                                                    :attributes attributes
                                                    :hpd-levels hpd-levels})
      (continuous-tree-model/insert-attributes! db id attributes)
      (continuous-tree-model/insert-hpd-levels! db id hpd-levels)
      (continuous-tree-model/upsert-tree! db {:id id
                                              :status :ATTRIBUTES_AND_HPD_LEVELS_PARSED}))
    (catch Exception e
      (log/error "Exception" {:error e})
      (continuous-tree-model/upsert-tree! db {:id id
                                              :status :ERROR}))))

;; TODO : parse ct handler
;; TODO : upload results to S3
(defmethod handler :parse-continuous-tree
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (let [tree-object-key (str user-id "/" id ".tree")
        tree-file-path (str tmp-dir "/" tree-object-key)
        ;; do we have it cached on disk?
        _ (when-not (file-exists? tree-file-path)
            (aws-s3/download-file s3 bucket-name tree-object-key tree-file-path))

        ;; TODO : read settings from RDS
        {:keys [] :as tree} (continuous-tree-model/get-tree db id)

        _ (log/info "@@@ RDS" {:t tree})

        ;; call all setters
        ;; parser (doto (new ContinuousTreeParser)
        ;;            (.setTreeFilePath tree-file-path))

        ]







    ))

(defn start [{:keys [aws db] :as config}]
  (let [{:keys [workers-queue-url bucket-name]} aws
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        db (db/init db)
        context {:s3 s3
                 :db db
                 :bucket-name bucket-name}]
    (log/info "Starting worker listener")
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
