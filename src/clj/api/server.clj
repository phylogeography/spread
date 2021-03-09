(ns api.server
  (:require [api.auth :as auth]
            [api.db :as db]
            [api.mutations :as mutations]
            [api.resolvers :as resolvers]
            [api.subscriptions :as subscriptions]
            [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.pedestal :refer [inject]]
            [com.walmartlabs.lacinia.pedestal.subscriptions
             :as
             pedestal-subscriptions]
            [com.walmartlabs.lacinia.pedestal2 :as pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :refer [interceptor]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(declare server)

(defn auth-decorator [resolver-fn]
  (fn [{:keys [headers] :as application-context} args value]
    (if-let [user-id (auth/token->user-id (get headers "Authorization"))]
      (resolver-fn (merge application-context {:authed-user-id user-id}) args value)
      (throw (Exception. "Authorization required")))))

(defn resolver-map []
  {:mutation/googleLogin   mutations/google-login
   :mutation/getUploadUrls (auth-decorator mutations/get-upload-urls)

   :mutation/uploadContinuousTree       (auth-decorator mutations/upload-continuous-tree)
   :mutation/updateContinuousTree       (auth-decorator mutations/update-continuous-tree)
   :query/getContinuousTree             resolvers/get-continuous-tree
   :resolve/continuous-tree->attributes resolvers/continuous-tree->attributes
   :resolve/continuous-tree->hpd-levels resolvers/continuous-tree->hpd-levels
   :mutation/startContinuousTreeParser  (auth-decorator mutations/start-continuous-tree-parser)

   :mutation/uploadDiscreteTree       (auth-decorator mutations/upload-discrete-tree)
   :mutation/updateDiscreteTree       (auth-decorator mutations/update-discrete-tree)
   :query/getDiscreteTree             resolvers/get-discrete-tree
   :resolve/discrete-tree->attributes resolvers/discrete-tree->attributes
   :mutation/startDiscreteTreeParser  (auth-decorator mutations/start-discrete-tree-parser)

   :mutation/uploadTimeSlicer       (auth-decorator mutations/upload-time-slicer)
   :mutation/updateTimeSlicer       (auth-decorator mutations/update-time-slicer)
   :query/getTimeSlicer             resolvers/get-time-slicer
   :resolve/time-slicer->attributes resolvers/time-slicer->attributes
   :mutation/startTimeSlicerParser  (auth-decorator mutations/start-time-slicer-parser)

   :mutation/uploadBayesFactorAnalysis           (auth-decorator mutations/upload-bayes-factor-analysis)
   :mutation/updateBayesFactorAnalysis           (auth-decorator mutations/update-bayes-factor-analysis)
   :mutation/startBayesFactorParser              (auth-decorator mutations/start-bayes-factor-parser)
   :query/getBayesFactorAnalysis                 resolvers/get-bayes-factor-analysis
   :resolve/bayes-factor-analysis->bayes-factors resolvers/bayes-factor-analysis->bayes-factors

   })

(defn streamer-map []
  {:subscription/continuousTreeParserStatus (auth-decorator (subscriptions/create-continuous-tree-parser-status-sub))
   :subscription/discreteTreeParserStatus   (auth-decorator (subscriptions/create-discrete-tree-parser-status-sub))
   :subscription/bayesFactorParserStatus    (auth-decorator (subscriptions/create-bayes-factor-parser-status-sub))
   :subscription/timeSlicerParserStatus     (auth-decorator (subscriptions/create-time-slicer-parser-status-sub))})

(defn ^:private context-interceptor
  [extra-context]
  (interceptor
    {:name  ::extra-context
     :enter (fn [context]
              (assoc-in context [:request :lacinia-app-context] extra-context))}))

(defn ^:private interceptors
  [schema extra-context]
  (-> (pedestal/default-interceptors schema nil)
      (inject (context-interceptor extra-context) :after ::pedestal/inject-app-context)
      #_(inject graphql-response-interceptor :before ::pedestal/query-executor)
      ))

(defn ^:private subscription-interceptors
  [schema extra-context]
  (-> (pedestal-subscriptions/default-subscription-interceptors schema nil)
      (inject (context-interceptor extra-context) :after :com.walmartlabs.lacinia.pedestal.subscriptions/inject-app-context)))

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn stop [this]
  (http/stop this))

(defn start [{:keys [api aws db env google private-key] :as config}]
  (let [dev?                                    (= "dev" env)
        {:keys [port allowed-origins]}          api
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
                                                 :google google
                                                 :private-key private-key}
        compiled-schema                         (-> schema
                                                    (lacinia-util/attach-resolvers (resolver-map))
                                                    (lacinia-util/attach-streamers (streamer-map))
                                                    schema/compile)
        interceptors                            (interceptors compiled-schema context)
        subscription-interceptors               (subscription-interceptors compiled-schema context)
        ;; TODO : use /ide endpoint only when env = dev
        routes                                  (into #{["/api" :post interceptors :route-name ::api]
                                                        ["/ide" :get (pedestal/graphiql-ide-handler {:port port}) :route-name ::graphiql-ide]}
                                                      (pedestal/graphiql-asset-routes "/assets/graphiql"))
        opts                                    (cond-> {::http/routes routes
                                                         ::http/port port
                                                         ::http/type :jetty
                                                         ::http/join? false
                                                         ::http/allowed-origins   {:allowed-origins (fn [origin]
                                                                                                      (log/debug "checking allowed CORS" {:origin origin})
                                                                                                      (allowed-origins origin))}}
                                                  true (pedestal/enable-subscriptions compiled-schema {:subscriptions-path        "/ws"
                                                                                                       ;; The interval at which keep-alive messages are sent to the client
                                                                                                       :keep-alive-ms             60000 ;; one minute
                                                                                                       :subscription-interceptors subscription-interceptors
                                                                                                       })
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
