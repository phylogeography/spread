(ns api.mutations
  (:require [api.auth :as auth]
            [api.models.analysis :as analysis-model]
            [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.emailer.sendgrid :as sendgrid]
            [api.emailer.banned :refer [banned-domains]]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [api.models.user :as user-model]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [aws.utils :refer [s3-url->id]]
            [clj-http.client :as http]
            [clojure.string :as string]
            [shared.errors :as errors]
            [shared.time :as time]
            [shared.utils :refer [clj->gql decode-json file-extension new-uuid]]
            [taoensso.timbre :as log]))

(defn send-login-email [{:keys [db private-key sendgrid]} {email        :email
                                                           redirect-uri :redirectUri
                                                           :as          args} _]
  (try
    (log/info "send-login-email" args)
    (if-not (banned-domains (second (string/split email #"@")))
      (let [token                         (auth/generate-spread-email-token email private-key)
            {:keys [api-key template-id]} sendgrid]
        (sendgrid/send-email {:from                  "noreply@spreadviz.org"
                              :to                    email
                              :template-id           template-id
                              :dynamic-template-data {"header"       "Login to Spread"
                                                      "body"         "You requested a login link to Spread. Click on the button below"
                                                      "button-title" "Login"
                                                      "button-href"  (str redirect-uri "?auth=email&token=" token)}
                              :api-key               api-key
                              :print-mode?           false})
        (clj->gql {:status :OK}))
      (log/warn "Not sending to one of the blacklisted domains" args))
    (catch Exception e
      (log/error "Sending login email failed" {:error e})
      (throw e))))

;; TODO
(defn email-login [{:keys [db private-key]} {:keys [token] :as args} _]
  ;; check token
  ;; read email from token
  (try
    (log/info "email-login" args)



    (catch Exception e
      (log/error "Sending login email failed" {:error e})
      (throw e))))

(defn google-login [{:keys [google db private-key]} {code :code redirect-uri :redirectUri} _]
  (try
    (let [{:keys [client-id client-secret]} google
          _                                 (log/debug "google oauth2 config" google)
          {:keys [body]}                    (http/post "https://oauth2.googleapis.com/token"
                                                       {:form-params  {:code          code
                                                                       :client_id     client-id
                                                                       :client_secret client-secret
                                                                       :redirect_uri  redirect-uri
                                                                       :grant_type    "authorization_code"}
                                                        :content-type :json
                                                        :accept       :json})
          _                                 (log/debug "google oauth2 API response" {:body body})
          {:keys [id_token]}                (decode-json body)
          _                                 (log/debug "google id_token" {:token id_token})
          {:keys [email]}                   (auth/verify-google-token id_token client-id)
          _                                 (log/debug "user email from google id token" {:email email})
          {:keys [id]}                      (user-model/get-user-by-email db {:email email})]
      (log/info "google-login" {:email email :id id})
      (if id
        (clj->gql {:access-token (auth/generate-spread-access-token id private-key)})
        ;; create user if not exists
        (let [new-user-id (new-uuid)]
          (user-model/upsert-user db {:email email
                                      :id    new-user-id})
          (clj->gql {:access-token (auth/generate-spread-access-token new-user-id private-key)}))))
    (catch Exception e
      (log/error "Login with google failed" {:error e})
      (throw e))))

(defn get-upload-urls
  [{:keys [s3-presigner authed-user-id bucket-name]} {:keys [files]} _]
  (log/info "get-upload-urls" {:user/id authed-user-id :files files})
  (loop [files files
         urls  []]
    (if-let [file (first files)]
      (let [{:keys [extension]} file
            uuid                (new-uuid)]
        (recur (rest files)
               (conj urls (aws-s3/get-signed-url
                            s3-presigner
                            {:bucket-name bucket-name
                             :key         (str authed-user-id "/" uuid "." extension)}))))
      urls)))

(defn upload-continuous-tree [{:keys [sqs workers-queue-url authed-user-id db]}
                              {tree-file-url  :treeFileUrl
                               tree-file-name :treeFileName
                               :as            args} _]
  (log/info "upload-continuous-tree" {:user/id authed-user-id
                                      :args    args})
  (let [id            (s3-url->id tree-file-url authed-user-id)
        status        :UPLOADED
        readable-name (first (string/split tree-file-name #"\."))]
    (try
      ;; TODO : in a transaction
      (analysis-model/upsert! db {:id            id
                                  :user-id       authed-user-id
                                  :readable-name readable-name
                                  :created-on    (time/millis (time/now))
                                  :status        status
                                  :of-type       :CONTINUOUS_TREE})
      (continuous-tree-model/upsert! db {:id             id
                                         :tree-file-url  tree-file-url
                                         :tree-file-name tree-file-name})
      ;; sends message to the worker queue to parse hpd levels and attributes
      (aws-sqs/send-message sqs workers-queue-url {:message/type :continuous-tree-upload
                                                   :id           id
                                                   :user-id      authed-user-id})
      (clj->gql (continuous-tree-model/get-tree db {:id id}))
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn update-continuous-tree
  [{:keys [authed-user-id db]} {id                          :id
                                readable-name               :readableName
                                x-coordinate-attribute-name :xCoordinateAttributeName
                                y-coordinate-attribute-name :yCoordinateAttributeName
                                timescale-multiplier        :timescaleMultiplier
                                most-recent-sampling-date   :mostRecentSamplingDate
                                :or                         {timescale-multiplier     1.0}
                                :as                         args} _]
  (log/info "update continuous tree" {:user/id authed-user-id
                                      :args    args})
  (try
    (let [status :ARGUMENTS_SET]
      ;; TODO : in a transaction
      (continuous-tree-model/upsert! db {:id                          id
                                         :x-coordinate-attribute-name x-coordinate-attribute-name
                                         :y-coordinate-attribute-name y-coordinate-attribute-name
                                         :timescale-multiplier        timescale-multiplier
                                         :most-recent-sampling-date   most-recent-sampling-date})
      (analysis-model/upsert! db {:id            id
                                  :readable-name readable-name
                                  :status        status}))
    (clj->gql (continuous-tree-model/get-tree db {:id id}))
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn start-continuous-tree-parser
  [{:keys [db sqs workers-queue-url]} {id                          :id
                                       readable-name               :readableName
                                       x-coordinate-attribute-name :xCoordinateAttributeName
                                       y-coordinate-attribute-name :yCoordinateAttributeName
                                       timescale-multiplier        :timescaleMultiplier
                                       most-recent-sampling-date   :mostRecentSamplingDate
                                       :as                         args} _]
  (log/info "start-continuous-tree-parser" args)
  (let [status :QUEUED]
    (try
      ;; TODO : in a tx
      (continuous-tree-model/upsert! db {:id                          id
                                         :readable-name               readable-name
                                         :x-coordinate-attribute-name x-coordinate-attribute-name
                                         :y-coordinate-attribute-name y-coordinate-attribute-name
                                         :timescale-multiplier        timescale-multiplier
                                         :most-recent-sampling-date   most-recent-sampling-date})
      (analysis-model/upsert! db {:id     id
                                  :status status})
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-continuous-tree
                                                   :id           id})
      (clj->gql (continuous-tree-model/get-tree db {:id id}))
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn upload-discrete-tree [{:keys [sqs workers-queue-url authed-user-id db]}
                            {tree-file-url  :treeFileUrl
                             tree-file-name :treeFileName
                             :as            args} _]
  (log/info "upload-discrete-tree" {:user/id authed-user-id
                                    :args    args})
  (let [id (s3-url->id tree-file-url authed-user-id)]
    (try
      (let [status        :UPLOADED
            readable-name (first (string/split tree-file-name #"\."))]
        ;; TODO : in a transaction
        (analysis-model/upsert! db {:id            id
                                    :user-id       authed-user-id
                                    :readable-name readable-name
                                    :created-on    (time/millis (time/now))
                                    :status        status
                                    :of-type       :DISCRETE_TREE})
        (discrete-tree-model/upsert! db {:id             id
                                         :tree-file-url  tree-file-url
                                         :tree-file-name tree-file-name})
        ;; sends message to worker to parse attributes
        (aws-sqs/send-message sqs workers-queue-url {:message/type :discrete-tree-upload
                                                     :id           id
                                                     :user-id      authed-user-id})
        (clj->gql (discrete-tree-model/get-tree db {:id id})))
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn update-discrete-tree
  [{:keys [authed-user-id db]} {id                        :id
                                readable-name             :readableName
                                locations-file-url        :locationsFileUrl
                                locations-file-name       :locationsFileName
                                locations-attribute-name  :locationsAttributeName
                                timescale-multiplier      :timescaleMultiplier
                                most-recent-sampling-date :mostRecentSamplingDate
                                :or                       {timescale-multiplier 1.0}
                                :as                       args} _]
  (log/info "update discrete tree" {:user/id authed-user-id
                                    :args    args})
  (try
    (let [status :ARGUMENTS_SET]
      ;; in transaction
      (discrete-tree-model/upsert! db {:id                        id
                                       :locations-file-url        locations-file-url
                                       :locations-file-name       locations-file-name
                                       :locations-attribute-name  locations-attribute-name
                                       :timescale-multiplier      timescale-multiplier
                                       :most-recent-sampling-date most-recent-sampling-date})
      (analysis-model/upsert! db {:id            id
                                  :readable-name readable-name
                                  :status        status})
      (clj->gql (discrete-tree-model/get-tree db {:id id})))
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn start-discrete-tree-parser
  [{:keys [db sqs workers-queue-url]} {id                        :id
                                       readable-name             :readableName
                                       locations-attribute-name  :locationsAttributeName
                                       timescale-multiplier      :timescaleMultiplier
                                       most-recent-sampling-date :mostRecentSamplingDate
                                       :as                       args} _]
  (log/info "start-discrete-tree-parser" args)
  (let [status :QUEUED]
    (try
      ;; TODO : in a tx
      (discrete-tree-model/upsert! db {:id                        id
                                       :readable-name             readable-name
                                       :locations-attribute-name  locations-attribute-name
                                       :timescale-multiplier      timescale-multiplier
                                       :most-recent-sampling-date most-recent-sampling-date})
      (analysis-model/upsert! db {:id     id
                                  :status status})
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-discrete-tree
                                                   :id           id})
      (clj->gql (discrete-tree-model/get-tree db {:id id}))
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn upload-time-slicer [{:keys [authed-user-id db]}
                          {continuous-tree-id      :continuousTreeId
                           trees-file-url          :treesFileUrl
                           trees-file-name         :treesFileName
                           slice-heights-file-url  :sliceHeightsFileUrl
                           slice-heights-file-name :sliceHeightsFileName
                           :as                     args} _]
  (log/info "upload-time-slicer" {:user/id authed-user-id
                                  :args    args})
  (let [id     (s3-url->id trees-file-url authed-user-id)
        status :UPLOADED]
    (try
      ;; TODO : in a transaction
      (analysis-model/upsert! db {:id         id
                                  :user-id    authed-user-id
                                  :created-on (time/millis (time/now))
                                  :status     status
                                  :of-type    :TIME_SLICER})
      (time-slicer-model/upsert! db {:id                      id
                                     :continuous-tree-id      continuous-tree-id
                                     :trees-file-url          trees-file-url
                                     :trees-file-name         trees-file-name
                                     :slice-heights-file-url  slice-heights-file-url
                                     :slice-heights-file-name slice-heights-file-name})
      (clj->gql (time-slicer-model/get-time-slicer db {:id id}))
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn update-time-slicer
  [{:keys [authed-user-id db]} {id                                      :id
                                burn-in                                 :burnIn
                                relaxed-random-walk-rate-attribute-name :relaxedRandomWalkRateAttributeName
                                trait-attribute-name                    :traitAttributeName
                                contouring-grid-size                    :contouringGridSize
                                number-of-intervals                     :numberOfIntervals
                                hpd-level                               :hpdLevel
                                timescale-multiplier                    :timescaleMultiplier
                                most-recent-sampling-date               :mostRecentSamplingDate
                                :or                                     {timescale-multiplier 1.0
                                                                         contouring-grid-size 100
                                                                         number-of-intervals  10}
                                :as                                     args} _]
  (log/info "update time-slicer" {:user/id authed-user-id
                                  :args    args})
  (try
    (let [status :ARGUMENTS_SET]
      ;; TODO : in a transaction
      (time-slicer-model/upsert! db {:id                                      id
                                     :burn-in                                 burn-in
                                     :number-of-intervals                     number-of-intervals
                                     :relaxed-random-walk-rate-attribute-name relaxed-random-walk-rate-attribute-name
                                     :trait-attribute-name                    trait-attribute-name
                                     :contouring-grid-size                    contouring-grid-size
                                     :hpd-level                               hpd-level
                                     :timescale-multiplier                    timescale-multiplier
                                     :most-recent-sampling-date               most-recent-sampling-date})
      (analysis-model/upsert! db {:id     id
                                  :status status})
      {:id     id
       :status status})
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn upload-bayes-factor-analysis [{:keys [authed-user-id db]}
                                    {log-file-url  :logFileUrl
                                     log-file-name :logFileName
                                     :as           args} _]
  (log/info "upload-bayes-factor" {:user/id authed-user-id
                                   :args    args})
  (let [id (s3-url->id log-file-url authed-user-id)]
    (try
      (let [status        :UPLOADED
            readable-name (first (string/split log-file-name #"\."))]
        ;; TODO : in a transaction
        (analysis-model/upsert! db {:id            id
                                    :user-id       authed-user-id
                                    :readable-name readable-name
                                    :created-on    (time/millis (time/now))
                                    :status        status
                                    :of-type       :BAYES_FACTOR_ANALYSIS})
        (bayes-factor-model/upsert! db {:id            id
                                        :log-file-url  log-file-url
                                        :log-file-name log-file-name})
        (clj->gql (bayes-factor-model/get-bayes-factor-analysis db {:id id})))
      (catch Exception e
        (log/error "Exception occured" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn update-bayes-factor-analysis
  [{:keys [authed-user-id db]} {id                  :id
                                readable-name       :readableName
                                locations-file-url  :locationsFileUrl
                                locations-file-name :locationsFileName
                                number-of-locations :numberOfLocations
                                burn-in             :burnIn
                                :or                 {burn-in 0.1}
                                :as                 args} _]
  (log/info "update bayes factor analysis" {:user/id authed-user-id
                                            :args    args})
  (try
    (let [status :ARGUMENTS_SET]
      ;; TODO : in a transaction
      (bayes-factor-model/upsert! db {:id                  id
                                      :locations-file-url  locations-file-url
                                      :locations-file-name locations-file-name
                                      :number-of-locations number-of-locations
                                      :burn-in             burn-in})
      (analysis-model/upsert! db {:id            id
                                  :readable-name readable-name
                                  :status        status})
      (clj->gql (bayes-factor-model/get-bayes-factor-analysis db {:id id})))
    (catch Exception e
      (log/error "Exception occured" {:error e})
      (errors/handle-analysis-error! db id e))))

(defn start-bayes-factor-parser
  [{:keys [db sqs workers-queue-url]} {id            :id
                                       readable-name :readableName
                                       burn-in       :burnIn
                                       :as           args} _]
  (log/info "start-bayes-factor-parser" args)
  (let [status :QUEUED]
    (try
      ;; TODO : in a transaction
      (bayes-factor-model/upsert! db {:id            id
                                      :readable-name readable-name
                                      :burn-in       burn-in})
      (analysis-model/upsert! db {:id     id
                                  :status status})
      (aws-sqs/send-message sqs workers-queue-url {:message/type :parse-bayes-factors
                                                   :id           id})
      (clj->gql (bayes-factor-model/get-bayes-factor-analysis db {:id id}))
      (catch Exception e
        (log/error "Exception when sending message to worker" {:error e})
        (errors/handle-analysis-error! db id e)))))

(defn touch-analysis
  [{:keys [authed-user-id db]} {id :id :as args} _]
  (log/info "touch-analysis" {:user/id authed-user-id
                              :args    args})
  (try
    (analysis-model/touch-analysis db {:id id})
    {:id    id
     :isNew false}
    (catch Exception e
      (log/error "Exception occured when marking analysis as touched" {:analysis/id id
                                                                       :error       e}))))

(defn- delete-s3-object! [{:keys [s3 bucket url user-id]}]
  (let [object-id  (s3-url->id url user-id)
        extension  (file-extension url)
        object-key (str user-id "/" object-id extension)]
    (log/info "delete-s3-object" {:object-key object-key})
    (aws-s3/delete-object s3 {:bucket bucket :key object-key})
    object-key))

(defn- delete-continuous-tree-analysis! [{:keys [id db s3 bucket-name user-id]}]
  (let [{:keys [tree-file-url output-file-url]} (continuous-tree-model/get-tree db {:id id})
        {trees-file-url :trees-file-url time-slicer-analysis-id :id}
        (time-slicer-model/get-time-slicer-by-continuous-tree-id db {:continuous-tree-id id})]
    (log/info "delete-continuous-tree-analysis" {:tree-file-url   tree-file-url
                                                 :output-file-url output-file-url
                                                 :trees-file-url  trees-file-url})
    (doseq [url (remove nil? [tree-file-url output-file-url trees-file-url])]
      (delete-s3-object! {:url url :user-id user-id :s3 s3 :bucket bucket-name}))
    ;; TODO : in a transaction
    (analysis-model/delete-analysis db {:id id})
    (when time-slicer-analysis-id
      (analysis-model/delete-analysis db {:id time-slicer-analysis-id}))))

(defn- delete-discrete-tree-analysis! [{:keys [id db s3 bucket-name user-id]}]
  (let [{:keys [tree-file-url locations-file-url output-file-url]}
        (discrete-tree-model/get-tree db {:id id})]
    (doseq [url (remove nil? [tree-file-url locations-file-url output-file-url])]
      (delete-s3-object! {:url url :user-id user-id :s3 s3 :bucket bucket-name}))
    (analysis-model/delete-analysis db {:id id})))

(defn- delete-bayes-factor-analysis! [{:keys [id db s3 bucket-name user-id]}]
  (let [{:keys [log-file-url locations-file-url output-file-url]}
        (bayes-factor-model/get-bayes-factor-analysis db {:id id})]
    (doseq [url (remove nil? [log-file-url locations-file-url output-file-url])]
      (delete-s3-object! {:url url :user-id user-id :s3 s3 :bucket bucket-name}))
    (analysis-model/delete-analysis db {:id id})))

(defn- delete-time-slicer-analysis! [{:keys [id db s3 bucket-name user-id]}]
  (let [{:keys [trees-file-url output-file-url]} (time-slicer-model/get-time-slicer db {:id id})]
    (log/info "delete-time-slicer-analysis" {:trees-file-url  trees-file-url
                                             :output-file-url output-file-url})
    (doseq [url (remove nil? [output-file-url trees-file-url])]
      (delete-s3-object! {:url url :user-id user-id :s3 s3 :bucket bucket-name}))
    (analysis-model/delete-analysis db {:id id})))

;; TODO : add tests for this mutation
(defn delete-analysis
  [{:keys [authed-user-id db s3 bucket-name]} {id :id :as args} _]
  (try
    (let [_                 (log/info "delete-analysis" {:user/id authed-user-id
                                                         :args    args})
          {:keys [of-type]} (analysis-model/get-analysis db {:id id})
          args-map          {:id id :db db :s3 s3 :bucket-name bucket-name :user-id authed-user-id}]
      (case (keyword of-type)
        :CONTINUOUS_TREE       (delete-continuous-tree-analysis! args-map)
        :DISCRETE_TREE         (delete-discrete-tree-analysis! args-map)
        :BAYES_FACTOR_ANALYSIS (delete-bayes-factor-analysis! args-map)
        :TIME_SLICER           (delete-time-slicer-analysis! args-map)
        (log/error "Unknown analysis type" {:of-type of-type :id id}))
      {:id id})
    (catch Exception e
      (log/error "Exception occured when deleting analysis" {:analysis/id id
                                                             :error       e}))))

(defn delete-file [{:keys [authed-user-id s3 bucket-name]} {url :url :as args} _]
  (try
    (let [_          (log/info "delete-file" {:user/id authed-user-id
                                              :args    args})
          object-key (delete-s3-object! {:url url :user-id authed-user-id :s3 s3 :bucket bucket-name})]
      {:key object-key})
    (catch Exception e
      (log/error "Exception occured when deleting file" {:url   url
                                                         :error e}))))

(defn- delete-user-data!
  [{:keys [user-id db s3 bucket-name]}]
  (let [user-objects (:Contents (aws-s3/list-objects s3 {:bucket bucket-name
                                                         :prefix user-id}))]
    (aws-s3/delete-objects s3 {:bucket bucket-name :objects user-objects})
    (analysis-model/delete-all-user-analysis db {:user-id user-id})))

(defn delete-user-data
  [{:keys [authed-user-id db s3 bucket-name]} _ _]
  (try
    (log/info "delete-user-data" {:user/id authed-user-id})
    (delete-user-data! {:user-id authed-user-id :db db :s3 s3 :bucket-name bucket-name})
    (clj->gql {:user-id authed-user-id})
    (catch Exception e
      (log/error "Exception occured when deleting user data" {:user/id authed-user-id
                                                              :error   e}))))

(defn delete-user-account
  [{:keys [authed-user-id db s3 bucket-name]} _ _]
  (try
    (log/info "delete-user-account" {:user/id authed-user-id})
    (delete-user-data! {:user-id authed-user-id :db db :s3 s3 :bucket-name bucket-name})
    (user-model/delete-user db {:id authed-user-id})
    (clj->gql {:user-id authed-user-id})
    (catch Exception e
      (log/error "Exception occured when deleting user account" {:user/id authed-user-id
                                                                 :error   e}))))
