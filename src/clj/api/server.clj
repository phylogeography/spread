(ns api.server
  (:require [api.auth :as auth]
            [api.db :as db]
            [api.mutations :as mutations]
            [api.resolvers :as resolvers]
            [api.scalars :as scalars]
            [api.subscriptions :as subscriptions]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [com.walmartlabs.lacinia.pedestal :refer [inject]]
            [com.walmartlabs.lacinia.pedestal.subscriptions
             :as
             pedestal-subscriptions]
            [com.walmartlabs.lacinia.pedestal2 :as pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.error :as interceptor.error]
            [mount.core :as mount :refer [defstate]]
            [next.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(declare server)

;; NOTE: this is not ideal as every copy of the ECS service will maintain it's own copy
;; It would be best to persist it to the RDB
;; however as we are currently running only one task per service it will suffice
(def ips (atom {}))

;; 10 minutes
;; if within this time period the same IP is seen 4 times it will be banned
(def offending-time-window-length (* 10 60 1000))

;; 60 minutes
;; ip is banned for this long
(def jail-time (* 60 60 1000))

(defn init-state [ip]
  {:ip ip :timestamps [] :state :free})

(defn transition [state now]
  (let [timestamps           (:timestamps state)
        last-timestamp       (last timestamps)
        ban-lift-time        (+ last-timestamp jail-time)
        offending-timestamps (filter #(<= (- last-timestamp %) offending-time-window-length) timestamps)]
    (case (:state state)
      :free               (if (> (count offending-timestamps) 4)
                            (assoc state :state :temporarily-jailed :jail-lift-time ban-lift-time)
                            (assoc state :state :free))
      :temporarily-jailed (if (> now (:jail-lift-time state))
                            (assoc state :state :free :timestamps offending-timestamps :jail-lift-time nil)
                            state))))

(defn update-ip-state! [ip new-state]
  (swap! ips assoc ip new-state))

(defn ip-jail-decorator [resolver-fn]
  (fn [{{x-forwarded-for         "x-forwarded-for"
         headers.x-forwarded-for "headers.x-forwarded-for"} :headers :as context} args value]
    (let [ip            (or x-forwarded-for headers.x-forwarded-for "127.0.0.1")
          now           (System/currentTimeMillis)
          current-state (or (@ips ip) (init-state ip))
          _             (log/info "verifying IP state" current-state)
          current-state (update current-state :timestamps conj now)
          new-state     (transition current-state now)
          _             (log/info "updating IP state" new-state)]
      (case (:state new-state)
        :free               (do
                              (update-ip-state! ip new-state)
                              (resolver-fn context args value))
        :temporarily-jailed (do
                              (update-ip-state! ip new-state)
                              (throw (Exception. "Too many consecutive requests. IP banned")))))))

(defn auth-decorator [resolver-fn]
  (fn [{:keys [access-token public-key] :as context} args value]
    (log/info "verifying auth token claims" {:access-token access-token})
    (if access-token
      ;; this will throw an exception when token is invalid or expired
      (let [{:keys [sub] :as claims} (auth/verify-token {:token      access-token
                                                         :public-key public-key
                                                         :claims     {:iss "spread"
                                                                      :aud "spread-client"}})]
        (log/info "verified token claims" claims)
        (resolver-fn (merge context {:authed-user-id sub}) args value))
      (throw (Exception. "Authorization required")))))

(defn tx-decorator
  "Wraps the request in a transaction.
  Replaces the :db key with a transaction (This works because our SQL execution works independently of passing a data-source, connection or transaction).
  Commits before sending the response or rollback the entire thing if anything throws."
  [resolver-fn]
  (fn [{:keys [db] :as context} args value]
    (jdbc/with-transaction [tx db {}]
      (resolver-fn (merge context {:db tx}) args value))))

(def mutation-decorator (comp auth-decorator tx-decorator))

(defn scalar-map
  []
  {:scalar/parse-big-int     scalars/parse-big-int
   :scalar/serialize-big-int scalars/serialize-big-int})

(defn resolver-map []
  {:mutation/googleLogin    mutations/google-login
   :mutation/sendLoginEmail (ip-jail-decorator mutations/send-login-email)
   :mutation/emailLogin     mutations/email-login
   :mutation/getUploadUrls  (mutation-decorator mutations/get-upload-urls)

   :query/pong              resolvers/pong
   :resolve/pong->status    resolvers/pong->status
   :query/getAuthorizedUser (auth-decorator resolvers/get-authorized-user)

   :resolve/custom-map       resolvers/tree->custom-map
   :mutation/uploadCustomMap (mutation-decorator mutations/upload-custom-map)
   :mutation/deleteCustomMap (mutation-decorator mutations/delete-custom-map)

   :mutation/uploadContinuousTree        (mutation-decorator mutations/upload-continuous-tree)
   :mutation/updateContinuousTree        (mutation-decorator mutations/update-continuous-tree)
   :query/getContinuousTree              resolvers/get-continuous-tree
   :resolve/continuous-tree->attributes  resolvers/continuous-tree->attributes
   :resolve/continuous-tree->time-slicer resolvers/continuous-tree->time-slicer
   :mutation/startContinuousTreeParser   (mutation-decorator mutations/start-continuous-tree-parser)

   :mutation/uploadDiscreteTree       (mutation-decorator mutations/upload-discrete-tree)
   :mutation/updateDiscreteTree       (mutation-decorator mutations/update-discrete-tree)
   :query/getDiscreteTree             resolvers/get-discrete-tree
   :resolve/discrete-tree->attributes resolvers/discrete-tree->attributes
   :mutation/startDiscreteTreeParser  (mutation-decorator mutations/start-discrete-tree-parser)

   :mutation/uploadTimeSlicer       (mutation-decorator mutations/upload-time-slicer)
   :mutation/updateTimeSlicer       (mutation-decorator mutations/update-time-slicer)
   :resolve/time-slicer->attributes resolvers/time-slicer->attributes

   :mutation/uploadBayesFactorAnalysis           (mutation-decorator mutations/upload-bayes-factor-analysis)
   :mutation/updateBayesFactorAnalysis           (mutation-decorator mutations/update-bayes-factor-analysis)
   :mutation/startBayesFactorParser              (mutation-decorator mutations/start-bayes-factor-parser)
   :query/getBayesFactorAnalysis                 resolvers/get-bayes-factor-analysis
   :resolve/bayes-factor-analysis->bayes-factors resolvers/bayes-factor-analysis->bayes-factors

   :query/getUserAnalysis       (auth-decorator resolvers/get-user-analysis)
   :resolve/analysis->error     resolvers/analysis->error
   :mutation/touchAnalysis      (mutation-decorator mutations/touch-analysis)
   :mutation/deleteFile         (mutation-decorator mutations/delete-file)
   :mutation/deleteAnalysis     (mutation-decorator mutations/delete-analysis)
   :mutation/deleteUserData     (mutation-decorator mutations/delete-user-data)
   :mutation/deleteUserAccount  (mutation-decorator mutations/delete-user-account)
   :resolve/tree->user-analysis resolvers/tree->user-analysis})

(defn streamer-map []
  {:subscription/parserStatus (auth-decorator (subscriptions/create-analysis-status-sub))})

(defn- context-interceptor
  [extra-context]
  (interceptor
    {:name  ::extra-context
     :enter (fn [context]
              (assoc-in context [:request :lacinia-app-context] extra-context))}))

(defn- auth-interceptor
  "Adds the JWT access-token located in the request Authorization header to the applications context
  this token is then validated by the `auth-decorator` that wraps resolvers/mutations/subscriptions
  that need to authenticate the user."
  []
  (interceptor
    {:name ::auth-interceptor
     :enter
     (fn [{{:keys [headers uri request-method]} :request :as context}]
       (if-not (and (= uri "/api")
                    (= request-method :post)) ;; Authenticate only GraphQL endpoint
         context
         (let [access-token (some-> headers
                                    (get "authorization")
                                    (string/split #"Bearer ")
                                    last
                                    string/trim)]
           (assoc-in context [:request :lacinia-app-context :access-token] access-token))))}))

(defn- request-headers-interceptor
  []
  (interceptor
    {:name ::request-headers
     :enter
     (fn [{{:keys [headers]} :request :as context}]
       (assoc-in context [:request :lacinia-app-context :headers] headers))}))

(defn- ws-auth-interceptor
  "Extracts access token from the connection parameters."
  []
  (interceptor
    {:name ::ws-auth-interceptor
     :enter
     (fn [{:keys [connection-params] :as context}]
       (log/debug "ws-auth-interceptor" connection-params)
       (let [access-token (some-> connection-params
                                  (get :Authorization)
                                  (string/split #"Bearer ")
                                  last
                                  string/trim)]
         (assoc-in context [:request :lacinia-app-context :access-token] access-token)))}))

(defn- interceptors
  [schema extra-context]
  (-> (pedestal/default-interceptors schema nil)
      (inject (context-interceptor extra-context) :after ::pedestal/inject-app-context)
      (inject (auth-interceptor) :after ::extra-context)
      (inject (request-headers-interceptor) :after ::auth-interceptor)))

(defn- subscription-interceptors
  [schema extra-context]
  (-> (pedestal-subscriptions/default-subscription-interceptors schema nil)
      (inject (context-interceptor extra-context) :after :com.walmartlabs.lacinia.pedestal.subscriptions/inject-app-context)
      (inject (ws-auth-interceptor) :after ::extra-context)))

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn stop [this]
  (http/stop this))

(def ^:private error-interceptor
  (interceptor.error/error-dispatch
    [context exception]
    :else
    (let [relevant-context (select-keys context
                                        [:interceptor
                                         :stage
                                         :execution-id
                                         :exception-type])]
      (log/error "Intercepted an error" {:context relevant-context
                                         :error   exception})
      (assoc context :io.pedestal.interceptor.chain/error exception))))

(def ^:private common-interceptors
  [error-interceptor (body-params/body-params) http/json-body])

(defn- healthcheck-response [_]
  {:body   {"status" "OK"}
   :status 200})

(defn start [{:keys [api aws db env google sendgrid public-key private-key] :as config}]
  (let [dev?                                    (= "dev" env)
        {:keys [port host allowed-origins]}     api
        {:keys [workers-queue-url bucket-name]} aws
        schema                                  (load-schema)
        sqs                                     (aws-sqs/create-client aws)
        s3                                      (aws-s3/create-client aws)
        s3-presigner                            (aws-s3/create-presigner aws)
        db                                      (db/init db)
        context                                 {:sqs               sqs
                                                 :s3                s3
                                                 :s3-presigner      s3-presigner
                                                 :db                db
                                                 :workers-queue-url workers-queue-url
                                                 :bucket-name       bucket-name
                                                 :google            google
                                                 :sendgrid          sendgrid
                                                 :private-key       private-key
                                                 :public-key        public-key}
        compiled-schema                         (-> schema
                                                    (lacinia-util/attach-scalar-transformers (scalar-map))
                                                    (lacinia-util/attach-resolvers (resolver-map))
                                                    (lacinia-util/attach-streamers (streamer-map))
                                                    schema/compile)
        interceptors                            (interceptors compiled-schema context)
        subscription-interceptors               (subscription-interceptors compiled-schema context)
        routes                                  #{["/api" :post interceptors :route-name ::api]
                                                  ["/healthcheck"
                                                   :get
                                                   (conj common-interceptors `healthcheck-response)
                                                   :route-name
                                                   ::healthcheck]}
        opts                                    (cond-> {::http/routes routes
                                                         ::http/port port
                                                         ::http/host host
                                                         ::http/type :jetty
                                                         ::http/join? false
                                                         ::http/allowed-origins   {:allowed-origins
                                                                                   (fn [origin]
                                                                                     (log/debug "checking allowed CORS" {:origin origin})
                                                                                     (allowed-origins origin))}}
                                                  true (pedestal/enable-subscriptions compiled-schema {:subscriptions-path        "/ws"
                                                                                                       ;; The interval at which keep-alive messages are sent to the client
                                                                                                       :keep-alive-ms             60000 ;; one minute
                                                                                                       :subscription-interceptors subscription-interceptors})
                                                  dev? (merge {:env                  (keyword env)
                                                               ::http/secure-headers nil}))
        runnable-service                        (-> (http/create-server opts)
                                                    http/start)]

    (log/info "Starting server" config)

    (when-not (contains? (set (:Buckets (aws-s3/list-buckets s3))) bucket-name)
      (aws-s3/create-bucket s3 {:bucket-name bucket-name}))

    (http/start runnable-service)
    runnable-service))

(defstate server
  :start (start (mount/args))
  :stop (stop server))
