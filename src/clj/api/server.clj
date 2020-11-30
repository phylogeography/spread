(ns api.server
  (:require [api.auth :as auth]
            [aws.s3 :as aws-s3]
            [shared.utils :refer [new-uuid]]
            [aws.sqs :as aws-sqs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.pedestal2 :as pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [io.pedestal.http :as http]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(defn get_upload_urls
  [{:keys [s3-presigner authed-user-id bucket-name] :as ctx} {:keys [files] :as args} _]

  (log/info "get_upload_urls" {:user/id authed-user-id :files files})

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

;; TODO : read status (from RDS)
(defn get_parser_execution
  [_ {:keys [id] :as args} _]
  (log/info "get_parser_execution" {:a args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :SUCCEEDED
   :output "s3://spread-dev-uploads/4d07edcf-4b4b-4190-8cea-38daece8d4aa"})

;; TODO : message schema
;; TODO : invoke parser
(defn start_parser_execution
  [{:keys [sqs workers-queue-url]} args _]
  (log/info "start_parser_execution" {:a args})
  (aws-sqs/send-message sqs workers-queue-url {:tree "s3://bla/bla"})

  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :QUEUED})

(defn auth-decorator [resolver-fn]
  (fn [{:keys [headers] :as application-context} args value]
    (if-let [user-id (auth/token->user-id (get headers "Authorization"))]
      (resolver-fn (merge application-context {:authed-user-id user-id}) args value)
      (throw (Exception. "Authorization required")))))

(defn context-decorator [resolver-fn context]
  (fn [application-context args value]
    (resolver-fn (merge application-context context) args value)))

(defn resolver-map [context]
  {:query/get_parser_execution get_parser_execution
   :mutation/start_parser_execution (context-decorator start_parser_execution context)
   :mutation/get_upload_urls (auth-decorator (context-decorator get_upload_urls context))
   })

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn stop [this]
  (http/stop this))

(defn start [{:keys [api aws] :as config}]
  (let [{:keys [port]} api
        {:keys [workers-queue-url bucket-name]} aws
        schema (load-schema)
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        s3-presigner (aws-s3/create-presigner aws)
        context {:sqs sqs
                 :s3 s3
                 :s3-presigner s3-presigner
                 :workers-queue-url workers-queue-url
                 :bucket-name bucket-name}
        compiled-schema (-> schema
                            (lacinia-util/attach-resolvers (resolver-map context))
                            schema/compile)
        service (pedestal/default-service compiled-schema {:port (Integer/parseInt port)})
        runnable-service (http/create-server service)]
    (log/info "Starting server" config)

    (when-not (contains? (set (:Buckets (aws-s3/list-buckets s3))) bucket-name)
      (aws-s3/create-bucket s3 {:bucket-name bucket-name}))

    (http/start runnable-service)
    runnable-service))

(defstate server
  :start (start (mount/args))
  :stop (stop server))
