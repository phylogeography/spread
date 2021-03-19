(ns worker.listener
  (:require [api.db :as db]
            [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [mount.core :as mount :refer [defstate]]
            [shared.utils :refer [file-exists? round]]
            [taoensso.timbre :as log])
  (:import [com.spread.parsers BayesFactorParser ContinuousTreeParser DiscreteTreeParser TimeSlicerParser]
           [com.spread.progress IProgressObserver IProgressReporter]))

(declare listener)

(defonce tmp-dir "/tmp")

(defn new-progress-handler [callback]
  (proxy [IProgressObserver] []
    (handleProgress [progress]
      (callback progress))))

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
          tree-file-path  (str tmp-dir "/" tree-object-key)
          _               (aws-s3/download-file s3 {:bucket    bucket-name
                                                    :key       tree-object-key
                                                    :dest-path tree-file-path})
          parser          (doto (new DiscreteTreeParser)
                            (.setTreeFilePath tree-file-path))
          attributes      (json/read-str (.parseAttributes parser))]
      (log/info "Parsed attributes" {:id         id
                                     :attributes attributes})
      (discrete-tree-model/insert-attributes! db id attributes)
      (discrete-tree-model/upsert-status! db {:tree-id id
                                              :status  :ATTRIBUTES_PARSED}))
    (catch Exception e
      (log/error "Exception when handling discrete-tree-upload" {:error e})
      (discrete-tree-model/upsert-status! db {:tree-id id
                                              :status  :ERROR}))))

(defmethod handler :parse-discrete-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-discrete-tree" args)
  (try
    (let [{:keys [user-id location-attribute-name
                  timescale-multiplier most-recent-sampling-date
                  locations-file-url]}
          (discrete-tree-model/get-tree db {:id id})
          ;; TODO: parse extension
          tree-object-key      (str user-id "/" id ".tree")
          tree-file-path       (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _                    (when-not (file-exists? tree-file-path)
                                 (aws-s3/download-file s3 {:bucket    bucket-name
                                                           :key       tree-object-key
                                                           :dest-path tree-file-path}))
          locations-file-id    (s3-url->id locations-file-url user-id)
          ;; TODO: parse extension
          locations-object-key (str user-id "/" locations-file-id ".txt")
          locations-file-path  (str tmp-dir "/" locations-object-key)
          ;; is it cached on disk?
          _                    (when-not (file-exists? locations-file-path)
                                 (aws-s3/download-file s3 {:bucket    bucket-name
                                                           :key       locations-object-key
                                                           :dest-path locations-file-path}))
          progress-handler     (new-progress-handler (fn [progress]
                                                       (let [progress (round 2 progress)]
                                                         (when (= (mod progress 0.1) 0.0)
                                                           (log/debug "discrete tree progress" {:id       id
                                                                                                :progress progress})
                                                           (discrete-tree-model/upsert-status! db {:tree-id  id
                                                                                                   :status   :RUNNING
                                                                                                   :progress progress})))))
          parser               (doto (new DiscreteTreeParser)
                                 (.setTreeFilePath tree-file-path)
                                 (.setLocationsFilePath locations-file-path)
                                 (.setLocationTraitAttributeName location-attribute-name)
                                 (.setTimescaleMultiplier timescale-multiplier)
                                 (.setMostRecentSamplingDate most-recent-sampling-date)
                                 (.registerProgressObserver progress-handler))
          output-object-key    (str user-id "/" id ".json")
          output-object-path   (str tmp-dir "/" output-object-key)
          _                    (spit output-object-path (.parse parser) :append false)
          _                    (aws-s3/upload-file s3 {:bucket    bucket-name
                                                       :key       output-object-key
                                                       :file-path output-object-path})
          url                  (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (discrete-tree-model/update! db {:id              id
                                       :output-file-url url})
      (discrete-tree-model/upsert-status! db {:tree-id id
                                              :status  :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-discrete-tree" {:error e})
      (discrete-tree-model/upsert-status! db {:tree-id id
                                              :status  :ERROR}))))

(defmethod handler :continuous-tree-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling continuous-tree-upload" args)
  (try
    (let [;; TODO: parse extension
          tree-object-key         (str user-id "/" id ".tree")
          tree-file-path          (str tmp-dir "/" tree-object-key)
          _                       (aws-s3/download-file s3 {:bucket    bucket-name
                                                            :key       tree-object-key
                                                            :dest-path tree-file-path})
          parser                  (doto (new ContinuousTreeParser)
                                    (.setTreeFilePath tree-file-path))
          [attributes hpd-levels] (json/read-str (.parseAttributesAndHpdLevels parser))]
      (log/info "Parsed attributes and hpd-levels" {:id         id
                                                    :attributes attributes
                                                    :hpd-levels hpd-levels})
      ;; TODO : in a transaction
      (continuous-tree-model/insert-attributes! db id attributes)
      (continuous-tree-model/insert-hpd-levels! db id hpd-levels)
      (continuous-tree-model/upsert-status! db {:tree-id id
                                                :status  :ATTRIBUTES_AND_HPD_LEVELS_PARSED}))
    (catch Exception e
      (log/error "Exception when handling continuous-tree-upload" {:error e})
      (continuous-tree-model/upsert-status! db {:tree-id id
                                                :status  :ERROR}))))

(defmethod handler :parse-continuous-tree
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-continuous-tree" args)
  (try
    (let [{:keys [user-id x-coordinate-attribute-name y-coordinate-attribute-name
                  hpd-level has-external-annotations timescale-multiplier
                  most-recent-sampling-date]}
          (continuous-tree-model/get-tree db {:id id})
          ;; TODO: parse extension
          tree-object-key    (str user-id "/" id ".tree")
          tree-file-path     (str tmp-dir "/" tree-object-key)
          ;; is it cached on disk?
          _                  (when-not (file-exists? tree-file-path)
                               (aws-s3/download-file s3 {:bucket    bucket-name
                                                         :key       tree-object-key
                                                         :dest-path tree-file-path}))
          progress-handler   (new-progress-handler (fn [progress]
                                                     (let [progress (round 2 progress)]
                                                       (when (= (mod progress 0.1) 0.0)
                                                         (log/debug "continuous tree progress" {:id       id
                                                                                                :progress progress})
                                                         (continuous-tree-model/upsert-status! db {:tree-id  id
                                                                                                   :status   :RUNNING
                                                                                                   :progress progress})))))
          ;; call all setters
          parser             (doto (new ContinuousTreeParser)
                               (.setTreeFilePath tree-file-path)
                               (.setXCoordinateAttributeName x-coordinate-attribute-name)
                               (.setYCoordinateAttributeName y-coordinate-attribute-name)
                               (.setHpdLevel hpd-level)
                               (.hasExternalAnnotations has-external-annotations)
                               (.setTimescaleMultiplier timescale-multiplier)
                               (.setMostRecentSamplingDate most-recent-sampling-date)
                               (.registerProgressObserver progress-handler))
          output-object-key  (str user-id "/" id ".json")
          output-object-path (str tmp-dir "/" output-object-key)
          _                  (spit output-object-path (.parse parser) :append false)
          _                  (aws-s3/upload-file s3 {:bucket    bucket-name
                                                     :key       output-object-key
                                                     :file-path output-object-path})
          url                (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (continuous-tree-model/update! db {:id              id
                                         :output-file-url url})
      (continuous-tree-model/upsert-status! db {:tree-id id
                                                :status  :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-continuous-tree" {:error e})
      (continuous-tree-model/upsert-status! db {:tree-id id
                                                :status  :ERROR}))))

(defmethod handler :time-slicer-upload
  [{:keys [id user-id] :as args} {:keys [db s3 bucket-name]}]
  (log/info "handling timeslicer-upload" args)
  (try
    (let [;; TODO: parse extension
          trees-object-key         (str user-id "/" id ".trees")
          trees-file-path          (str tmp-dir "/" trees-object-key)
          _                        (aws-s3/download-file s3 {:bucket    bucket-name
                                                             :key       trees-object-key
                                                             :dest-path trees-file-path})
          parser                   (doto (new TimeSlicerParser)
                                     (.setTreesFilePath trees-file-path))
          [attributes trees-count] (json/read-str (.parseAttributesAndTreesCount parser))]
      (log/info "Parsed attributes and hpd-levels" {:id         id
                                                    :attributes attributes
                                                    :count      trees-count})
      ;; TODO : in a transaction
      (time-slicer-model/insert-attributes! db id attributes)
      (time-slicer-model/update! db {:id          id
                                     :trees-count trees-count})
      (time-slicer-model/upsert-status! db {:time-slicer-id id
                                            :status         :ATTRIBUTES_AND_TREES_COUNT_PARSED}))
    (catch Exception e
      (log/error "Exception when handling time-slicer-upload" {:error e})
      (time-slicer-model/upsert-status! db {:time-slicer-id id
                                            :status         :ERROR}))))

(defmethod handler :parse-time-slicer
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-time-slicer" args)
  (try
    (let [{:keys [user-id
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
          trees-object-key   (str user-id "/" id ".trees")
          trees-file-path    (str tmp-dir "/" trees-object-key)
          ;; is it cached on disk?
          _                  (when-not (file-exists? trees-file-path)
                               (aws-s3/download-file s3 {:bucket    bucket-name
                                                         :key       trees-object-key
                                                         :dest-path trees-file-path}))
          progress-handler   (new-progress-handler (fn [progress]
                                                     (let [progress (round 2 progress)]
                                                       (when (= (mod progress 0.1) 0.0)
                                                         (log/debug "time-slicer progress" {:id       id
                                                                                            :progress progress})
                                                         (time-slicer-model/upsert-status! db {:time-slicer-id id
                                                                                               :status         :RUNNING
                                                                                               :progress       progress})))))
          ;; call all setters
          parser             (doto (new TimeSlicerParser)
                               (.setTreesFilePath trees-file-path)
                               (.setBurnIn burn-in)
                               (.setRrwRateName relaxed-random-walk-rate-attribute-name)
                               (.setTraitName trait-attribute-name)
                               (.setNumberOfIntervals number-of-intervals)
                               (.setHpdLevel hpd-level)
                               (.setGridSize contouring-grid-size)
                               (.setTimescaleMultiplier timescale-multiplier)
                               (.setMostRecentSamplingDate most-recent-sampling-date)
                               (.registerProgressObserver progress-handler))
          output-object-key  (str user-id "/" id ".json")
          output-object-path (str tmp-dir "/" output-object-key)
          _                  (spit output-object-path (.parse parser) :append false)
          _                  (aws-s3/upload-file s3 {:bucket    bucket-name
                                                     :key       output-object-key
                                                     :file-path output-object-path})
          url                (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (time-slicer-model/update! db {:id              id
                                     :output-file-url url})
      (time-slicer-model/upsert-status! db {:time-slicer-id id
                                            :status         :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-time-slicer" {:error e})
      (time-slicer-model/upsert-status! db {:time-slicer-id id
                                            :status         :ERROR}))))

(defmethod handler :parse-bayes-factors
  [{:keys [id] :as args} {:keys [db s3 bucket-name aws-config]}]
  (log/info "handling parse-bayes-factors" args)
  (try
    (let [_                (bayes-factor-model/update! db {:id     id
                                                           :status :RUNNING})
          {:keys [user-id
                  locations-file-url
                  number-of-locations
                  burn-in]}
          (bayes-factor-model/get-bayes-factor-analysis db {:id id})
          ;; TODO: parse extension
          log-object-key   (str user-id "/" id ".log")
          log-file-path    (str tmp-dir "/" log-object-key)
          ;; is it cached on disk?
          _                (when-not (file-exists? log-file-path)
                             (aws-s3/download-file s3 {:bucket    bucket-name
                                                       :key       log-object-key
                                                       :dest-path log-file-path}))
          progress-handler (new-progress-handler (fn [progress]
                                                   (let [progress (round 2 progress)]
                                                     (when (= (mod progress 0.1) 0.0)
                                                       (log/debug "bayes factor progress" {:id       id
                                                                                           :progress progress})
                                                       (bayes-factor-model/upsert-status! db {:bayes-factor-analysis-id id
                                                                                              :status                   :RUNNING
                                                                                              :progress                 progress})))))
          parser
          (match [(nil? locations-file-url) (nil? number-of-locations)]

                 [true false]
                 (doto (new BayesFactorParser)
                   (.setLogFilePath log-file-path)
                   (.setNumberOfGeneratedLocations number-of-locations)
                   (.setBurnIn (double burn-in))
                   (.registerProgressObserver progress-handler))

                 [false true]
                 (let [locations-file-id    (s3-url->id locations-file-url user-id)
                       ;; TODO: parse extension
                       locations-object-key (str user-id "/" locations-file-id ".txt")
                       locations-file-path  (str tmp-dir "/" locations-object-key)
                       ;; is it cached on disk?
                       _                    (when-not (file-exists? locations-file-path)
                                              (aws-s3/download-file s3 {:bucket    bucket-name
                                                                        :key       locations-object-key
                                                                        :dest-path locations-file-path}))]
                   (doto (new BayesFactorParser)
                     (.setLogFilePath log-file-path)
                     (.setLocationsFilePath locations-file-path)
                     (.setBurnIn (double burn-in))
                     (.registerProgressObserver progress-handler)))

                 [true true]
                 (throw (ex-info "Bad input settings"
                                 {:why?   "Can't specify both `log-file-path` and `number-of-locations`"
                                  :where? ::parse-bayes-factors}))
                 [false false]
                 (throw (ex-info "Bad input settings"
                                 {:why?   "You need to specify one of `log-file-path` and `number-of-locations`"
                                  :where? ::parse-bayes-factors}))
                 :else (throw (Exception. "Unexpected error")))
          {:keys [bayesFactors spreadData]} (-> (.parse parser) (json/read-str :key-fn keyword))
          output-object-key                 (str user-id "/" id ".json")
          output-object-path                (str tmp-dir "/" output-object-key)
          _                                 (spit output-object-path (json/write-str spreadData) :append false)
          _                                 (aws-s3/upload-file s3 {:bucket    bucket-name
                                                                    :key       output-object-key
                                                                    :file-path output-object-path})
          url                               (aws-s3/build-url aws-config bucket-name output-object-key)]
      ;; TODO : in a transaction
      (bayes-factor-model/insert-bayes-factors db {:bayes-factor-analysis-id id
                                                   :bayes-factors            (json/write-str bayesFactors)})
      (bayes-factor-model/update! db {:id              id
                                      :output-file-url url})
      (bayes-factor-model/upsert-status! db {:bayes-factor-analysis-id id
                                             :status                   :SUCCEEDED}))
    (catch Exception e
      (log/error "Exception when handling parse-bayes-factors" {:error e})
      (bayes-factor-model/upsert-status! db {:bayes-factor-analysis-id id
                                             :status                   :ERROR}))))

(defn start [{:keys [aws db] :as config}]
  (let [{:keys [workers-queue-url bucket-name]} aws
        sqs                                     (aws-sqs/create-client aws)
        s3                                      (aws-s3/create-client aws)
        db                                      (db/init db)
        context                                 {:s3          s3
                                                 :db          db
                                                 :bucket-name bucket-name
                                                 :aws-config  aws}]
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
