(ns api.mutations
  (:require [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [api.models.bayes-factor :as bayes-factor-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [shared.utils :refer [new-uuid]]
            [taoensso.timbre :as log]))

(defn get-upload-urls
  [{:keys [s3-presigner authed-user-id bucket-name]} {:keys [files]} _]
  (log/info "get-upload-urls" {:user/id authed-user-id :files files})
  (loop [files files
         urls []]
    (if-let [file (first files)]
      (let [{:keys [extension]} file
            uuid (new-uuid)]
        (recur (rest files)
               (conj urls (aws-s3/get-signed-url
                           s3-presigner
                           {:bucket-name bucket-name
                            :key (str authed-user-id "/" uuid "." extension)}))))
      urls)))

(defn upload-continuous-tree [{:keys [sqs workers-queue-url authed-user-id db]}
                              {tree-file-url :treeFileUrl readable-name :readableName
                               :as args} _]
  (log/info "upload-continuous-tree" {:user/id authed-user-id
                                      :args args})
  (let [id (s3-url->id tree-file-url authed-user-id)
        status :TREE_UPLOADED
        continuous-tree {:id id
                         :readable-name readable-name
                         :user-id authed-user-id
                         :tree-file-url tree-file-url
                         :status status}]
    (try
      (continuous-tree-model/upsert! db continuous-tree)
      ;; sends message to worker to parse hpd levels and attributes
      (aws-sqs/send-message sqs workers-queue-url {:message/type :continuous-tree-upload
                                                   :id id
                                                   :user-id authed-user-id})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (continuous-tree-model/update! db {:id id
                                           :status :ERROR})))))

(defn update-continuous-tree
  [{:keys [authed-user-id db]} {id :id
                                readable-name :readableName
                                x-coordinate-attribute-name :xCoordinateAttributeName
                                y-coordinate-attribute-name :yCoordinateAttributeName
                                hpd-level :hpdLevel
                                has-external-annotations :hasExternalAnnotations
                                timescale-multiplier :timescaleMultiplier
                                most-recent-sampling-date :mostRecentSamplingDate
                                :or {has-external-annotations true
                                     timescale-multiplier 1.0}
                                :as args} _]
  (log/info "update continuous tree" {:user/id authed-user-id
                                      :args args})
  (try
    (let [status :PARSER_ARGUMENTS_SET]
      (continuous-tree-model/update! db {:id id
                                         :readable-name readable-name
                                         :x-coordinate-attribute-name x-coordinate-attribute-name
                                         :y-coordinate-attribute-name y-coordinate-attribute-name
                                         :hpd-level hpd-level
                                         :has-external-annotations has-external-annotations
                                         :timescale-multiplier timescale-multiplier
                                         :most-recent-sampling-date most-recent-sampling-date
                                         :status status})
      {:id id
       :status status})
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (continuous-tree-model/update! db {:id id
                                         :status :ERROR}))))

(defn start-continuous-tree-parser
  [{:keys [db sqs workers-queue-url]} {id :id :as args} _]
  (log/info "start-continuous-tree-parser" args)
  (let [status :QUEUED]
    (try
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-continuous-tree
                                                   :id id})
      (continuous-tree-model/update! db {:id id :status status})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (continuous-tree-model/update! db {:id id
                                           :status :ERROR})))))

(defn upload-discrete-tree [{:keys [sqs workers-queue-url authed-user-id db]}
                            {tree-file-url :treeFileUrl
                             locations-file-url :locationsFileUrl
                             readable-name :readableName
                             :as args} _]
  (log/info "upload-discrete-tree" {:user/id authed-user-id
                                    :args args})
  (let [id (s3-url->id tree-file-url authed-user-id)
        ;; NOTE: uploads mutation generates different ids for each uploaded file
        ;; _ (assert (= id (s3-url->id locations-file-url bucket-name authed-user-id)))
        status :TREE_AND_LOCATIONS_UPLOADED
        discrete-tree {:id id
                       :readable-name readable-name
                       :user-id authed-user-id
                       :tree-file-url tree-file-url
                       :locations-file-url locations-file-url
                       :status status}]
    (try
      (discrete-tree-model/upsert! db discrete-tree)
      ;; sends message to worker to parse attributes
      (aws-sqs/send-message sqs workers-queue-url {:message/type :discrete-tree-upload
                                                   :id id
                                                   :user-id authed-user-id})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (discrete-tree-model/update! db {:id id
                                         :status :ERROR})))))

(defn update-discrete-tree
  [{:keys [authed-user-id db]} {id :id
                                readable-name :readableName
                                location-attribute-name :locationAttributeName
                                timescale-multiplier :timescaleMultiplier
                                most-recent-sampling-date :mostRecentSamplingDate
                                :or {timescale-multiplier 1}
                                :as args} _]
  (log/info "update discrete tree" {:user/id authed-user-id
                                    :args args})
  (try
    (let [status :PARSER_ARGUMENTS_SET]
      (discrete-tree-model/update! db {:id id
                                       :readable-name readable-name
                                       :location-attribute-name location-attribute-name
                                       :timescale-multiplier timescale-multiplier
                                       :most-recent-sampling-date most-recent-sampling-date
                                       :status status})
      {:id id
       :status status})
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (discrete-tree-model/update! db {:id id
                                       :status :ERROR}))))

(defn start-discrete-tree-parser
  [{:keys [db sqs workers-queue-url]} {id :id :as args} _]
  (log/info "start-discrete-tree-parser" args)
  (let [status :QUEUED]
    (try
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-discrete-tree
                                                   :id id})
      (discrete-tree-model/update! db {:id id :status status})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (discrete-tree-model/update! db {:id id
                                         :status :ERROR})))))

(defn upload-time-slicer [{:keys [sqs workers-queue-url authed-user-id db]}
                          {trees-file-url :treesFileUrl readable-name :readableName
                           slice-heights-file-url :sliceHeightsFileUrl
                           :as args} _]
  (log/info "upload-time-slicer" {:user/id authed-user-id
                                  :args args})
  (let [id (s3-url->id trees-file-url authed-user-id)
        status :TREES_UPLOADED
        time-slicer {:id id
                     :readable-name readable-name
                     :user-id authed-user-id
                     :trees-file-url trees-file-url
                     :slice-heights-file-url slice-heights-file-url
                     :status status}]
    (try
      (time-slicer-model/upsert! db time-slicer)
      ;; sends message to the worker to parse attributes
      (aws-sqs/send-message sqs workers-queue-url {:message/type :time-slicer-upload
                                                   :id id
                                                   :user-id authed-user-id})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (time-slicer-model/update! db {:id id
                                       :status :ERROR})))))

(defn update-time-slicer
  [{:keys [authed-user-id db]} {id :id
                                readable-name :readableName
                                burn-in :burnIn
                                relaxed-random-walk-rate-attribute-name  :relaxedRandomWalkRateAttributeName
                                trait-attribute-name :traitAttributeName
                                contouring-grid-size :contouringGridSize
                                number-of-intervals :numberOfIntervals
                                hpd-level :hpdLevel
                                timescale-multiplier :timescaleMultiplier
                                most-recent-sampling-date :mostRecentSamplingDate
                                :or {timescale-multiplier 1.0
                                     contouring-grid-size 100
                                     number-of-intervals 10}
                                :as args} _]
  (log/info "update time-slicer" {:user/id authed-user-id
                                  :args args})
  (try
    (let [status :PARSER_ARGUMENTS_SET]
      (time-slicer-model/update! db {:id id
                                     :readable-name readable-name
                                     :burn-in burn-in
                                     :number-of-intervals number-of-intervals
                                     :relaxed-random-walk-rate-attribute-name relaxed-random-walk-rate-attribute-name
                                     :trait-attribute-name trait-attribute-name
                                     :contouring-grid-size contouring-grid-size
                                     :hpd-level hpd-level
                                     :timescale-multiplier timescale-multiplier
                                     :most-recent-sampling-date most-recent-sampling-date
                                     :status status})
      {:id id
       :status status})
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (time-slicer-model/update! db {:id id
                                     :status :ERROR}))))

(defn start-time-slicer-parser
  [{:keys [db sqs workers-queue-url]} {id :id :as args} _]
  (log/info "start-time-slicer-parser" args)
  (let [status :QUEUED]
    (try
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-time-slicer
                                                   :id id})
      (time-slicer-model/update! db {:id id :status status})
      {:id id
       :status status}
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (time-slicer-model/update! db {:id id
                                       :status :ERROR})))))

;; TODO
(defn upload-bayes-factor-analysis [{:keys [sqs workers-queue-url authed-user-id db]}
                                    {log-file-url       :logFileUrl
                                     locations-file-url :locationsFileUrl
                                     readable-name      :readableName
                                     burn-in            :burnIn
                                     :or                {burn-in 0.1}
                                     :as                args} _]
  (log/info "upload-bayes-factor" {:user/id authed-user-id
                                   :args    args})
  (let [id       (s3-url->id log-file-url authed-user-id)
        ;; NOTE: uploads mutation generates different ids for each uploaded file
        ;; _ (assert (= id (s3-url->id locations-file-url bucket-name authed-user-id)))
        status   :DATA_UPLOADED
        analysis {:id                 id
                  :readable-name      readable-name
                  :user-id            authed-user-id
                  :log-file-url       log-file-url
                  :locations-file-url locations-file-url
                  :burn-in            burn-in
                  :status             status}]
    (try
      (bayes-factor-model/upsert! db analysis)
      {:id     id
       :status status}
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (bayes-factor-model/update! db {:id     id
                                        :status :ERROR})))))

(defn update-bayes-factor-analysis
  [{:keys [authed-user-id db]} {id                 :id
                                readable-name      :readableName
                                locations-file-url :locationsFileUrl
                                burn-in            :burnIn
                                :or                {burn-in 0.1}
                                :as                args} _]
  (log/info "update discrete tree" {:user/id authed-user-id
                                    :args    args})
  (try
    (let [status :PARSER_ARGUMENTS_SET]
      (discrete-tree-model/update! db {:id                      id
                                       :readable-name           readable-name
                                       :burn-in            burn-in
                                       :status status})
      {:id     id
       :status status})
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (bayes-factor-model/update! db {:id     id
                                      :status :ERROR}))))

(defn start-bayes-factor-parser
  [{:keys [db sqs workers-queue-url]} {id :id :as args} _]
  (log/info "start-bayes-factor-parser" args)
  (let [status :QUEUED]
    (try
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-bayes-factors
                                                   :id           id})
      (bayes-factor-model/update! db {:id id :status status})
      {:id     id
       :status status}
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (bayes-factor-model/update! db {:id     id
                                        :status :ERROR})))))
