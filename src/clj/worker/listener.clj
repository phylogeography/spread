(ns worker.listener
  (:require [api.db :as db]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [api.models.bayes-factor :as bayes-factor-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [clojure.data.json :as json]
            [mount.core :as mount :refer [defstate]]
            [shared.utils :refer [file-exists?]]
            [taoensso.timbre :as log])
  (:import (com.spread.parsers ContinuousTreeParser)
           (com.spread.parsers DiscreteTreeParser)
           (com.spread.parsers TimeSlicerParser)
           (com.spread.parsers BayesFactorParser)))

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
    (let [;; TODO: parse extension
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
                  locations-file-url]}
          (discrete-tree-model/get-tree db {:id id})
          ;; TODO: parse extension
          tree-object-key (str user-id "/" id ".tree")
          tree-file-path (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? tree-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key tree-object-key
                                        :dest-path tree-file-path}))
          locations-file-id (s3-url->id locations-file-url user-id)
          ;; TODO: parse extension
          locations-object-key (str user-id "/" locations-file-id ".txt")
          locations-file-path (str tmp-dir "/" locations-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? locations-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key locations-object-key
                                        :dest-path locations-file-path}))
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
    (let [;; TODO: parse extension
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
      (continuous-tree-model/update! db {:id id
                                         :status :ATTRIBUTES_AND_HPD_LEVELS_PARSED}))
    (catch Exception e
      (log/error "Exception when handling continuous-tree-upload" {:error e})
      (continuous-tree-model/update! db {:id id
                                         :status :ERROR}))))

(defmethod handler :parse-continuous-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-continuous-tree" args)
  (try
    (let [_ (continuous-tree-model/update! db {:id id
                                               :status :RUNNING})
          {:keys [user-id x-coordinate-attribute-name y-coordinate-attribute-name
                  hpd-level has-external-annotations timescale-multiplier
                  most-recent-sampling-date]}
          (continuous-tree-model/get-tree db {:id id})
          ;; TODO: parse extension
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
      (continuous-tree-model/update! db {:id id
                                         :output-file-url url
                                         :status :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-continuous-tree" {:error e})
      (continuous-tree-model/update! db {:id id
                                         :status :ERROR}))))

(defmethod handler :time-slicer-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling timeslicer-upload" args)
  (try
    (let [;; TODO: parse extension
          trees-object-key (str user-id "/" id ".trees")
          trees-file-path (str tmp-dir "/" trees-object-key)
          _ (aws-s3/download-file s3 {:bucket bucket-name
                                      :key trees-object-key
                                      :dest-path trees-file-path})
          parser (doto (new TimeSlicerParser)
                   (.setTreesFilePath trees-file-path))
          [attributes trees-count] (json/read-str (.parseAttributesAndTreesCount parser))]
      (log/info "Parsed attributes and hpd-levels" {:id id
                                                    :attributes attributes
                                                    :count trees-count})
      (time-slicer-model/insert-attributes! db id attributes)
      (time-slicer-model/update! db {:id id
                                     :trees-count trees-count
                                     :status :ATTRIBUTES_AND_TREES_COUNT_PARSED}))
    (catch Exception e
      (log/error "Exception when handling time-slicer-upload" {:error e})
      (time-slicer-model/update! db {:id id
                                     :status :ERROR}))))

(defmethod handler :parse-time-slicer
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-time-slicer" args)
  (try
    (let [_ (time-slicer-model/update! db {:id id
                                           :status :RUNNING})
          {:keys [user-id
                  burn-in
                  relaxed-random-walk-rate-attribute-name
                  trait-attribute-name
                  number-of-intervals
                  contouring-grid-size
                  hpd-level
                  timescale-multiplier
                  most-recent-sampling-date]}
          (time-slicer-model/get-time-slicer db {:id id})
          ;; TODO: parse extension
          trees-object-key (str user-id "/" id ".trees")
          trees-file-path (str tmp-dir "/" trees-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? trees-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key trees-object-key
                                        :dest-path trees-file-path}))
          ;; call all setters
          parser (doto (new TimeSlicerParser)
                   (.setTreesFilePath trees-file-path)
                   (.setBurnIn burn-in)
                   (.setRrwRateName relaxed-random-walk-rate-attribute-name)
                   (.setTraitName trait-attribute-name)
                   (.setNumberOfIntervals number-of-intervals)
                   (.setHpdLevel hpd-level)
                   (.setGridSize contouring-grid-size)
                   (.setTimescaleMultiplier timescale-multiplier)
                   (.setMostRecentSamplingDate most-recent-sampling-date))
          output-object-key (str user-id "/" id ".json")
          output-object-path (str tmp-dir "/" output-object-key)
          _ (spit output-object-path (.parse parser) :append false)
          _ (aws-s3/upload-file s3 {:bucket bucket-name
                                    :key output-object-key
                                    :file-path output-object-path})
          url (aws-s3/build-url aws-config bucket-name output-object-key)]
      (time-slicer-model/update! db {:id id
                                     :output-file-url url
                                     :status :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-time-slicer" {:error e})
      (time-slicer-model/update! db {:id id
                                     :status :ERROR}))))

;; TODO
(defmethod handler :parse-bayes-factors
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-bayes-factors" args)
  (try
    (let [_ (bayes-factor-model/update! db {:id     id
                                            :status :RUNNING})
          _ (log/debug "@@@ before")
          {:keys [user-id
                  log-file-url
                  locations-file-url
                  burn-in]}
          (bayes-factor-model/get-bayes-factor-analysis db {:id id})
          ;; TODO: parse extension
          log-object-key (str user-id "/" id ".log")
          log-file-path (str tmp-dir "/" log-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? log-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key log-object-key
                                        :dest-path log-file-path}))

          locations-file-id (s3-url->id locations-file-url user-id)
          ;; ;; TODO: parse extension
          locations-object-key (str user-id "/" locations-file-id ".txt")
          locations-file-path (str tmp-dir "/" locations-object-key)
          ;; is it cached on disk?
          _ (when-not (file-exists? locations-file-path)
              (aws-s3/download-file s3 {:bucket bucket-name
                                        :key locations-object-key
                                        :dest-path locations-file-path}))
          parser (doto (new DiscreteTreeParser)
                   (.setTreeFilePath tree-file-path)
                   (.setLocationsFilePath locations-file-path)
                   (.setLocationTraitAttributeName location-attribute-name)
                   (.setTimescaleMultiplier timescale-multiplier)
                   (.setMostRecentSamplingDate most-recent-sampling-date))
          ;; output-object-key (str user-id "/" id ".json")
          ;; output-object-path (str tmp-dir "/" output-object-key)
          ;; _ (spit output-object-path (.parse parser) :append false)
          ;; _ (aws-s3/upload-file s3 {:bucket bucket-name
          ;;                           :key output-object-key
          ;;                           :file-path output-object-path})
          ;; url (aws-s3/build-url aws-config bucket-name output-object-key)
          ]
      #_(discrete-tree-model/update! db {:id id
                                       :output-file-url url
                                       :status :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-bayes-factors" {:error e})
      (bayes-factor-model/update! db {:id     id
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
