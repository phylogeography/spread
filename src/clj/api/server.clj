(ns api.server
  (:require
   [aws.sqs :as aws-sqs]
   [aws.s3 :as aws-s3]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia.pedestal2 :as pedestal]
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as lacinia-util]
   [io.pedestal.http :as http]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   ))

;; (defn get-urls
;;   [db s3 config authed-user-id files]
;;   (go-loop [files files
;;             urls []]
;;     (if-let [file (first files)]
;;       (let [{:keys [extension]} file
;;             uuid (<! (db/new-uuid db))]
;;         (recur (rest files)
;;                (conj urls (<!p (s3-api/get-signed-url
;;                                 s3
;;                                 "putObject"
;;                                 {:bucket (get-in config [:aws :bucket-name])
;;                                  :key (str authed-user-id "/" uuid "." extension)
;;                                  :expires 300})))))
;;       urls)))


(defn get_upload_urls
  [{:keys [s3]} {:keys [files] :as args} _]
  (log/debug "get_upload_urls" {:a args})
  {
   :urls ["foo" "bar"]
   })


(defn get_parser_execution
  [_ {:keys [id] :as args} _]
  (log/debug "get_parser_execution" {:a args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :SUCCEEDED
   :output "s3://spread-dev-uploads/4d07edcf-4b4b-4190-8cea-38daece8d4aa"})

;; TODO : message schema
(defn start_parser_execution
  [{:keys [sqs workers-queue-url]} args _]
  (log/debug "start_parser_execution" {:a args})
  (aws-sqs/send-message sqs workers-queue-url {:tree "s3://bla/bla"})

  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :QUEUED})

(defn context-decorator [resolver-fn context]
  (fn [application-context args value]
    (resolver-fn (merge application-context context) args value)))

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn resolver-map [context]
  {:query/get_parser_execution get_parser_execution
   :mutation/start_parser_execution (context-decorator start_parser_execution context)
   :mutation/get_upload_urls (context-decorator get_upload_urls context)
   })

(defn stop [this]
  (http/stop this))

;; TODO : ensure bucket exists
(defn start [{:keys [api aws] :as config}]
  (let [{:keys [port]} api
        {:keys [workers-queue-url bucket-name]} aws
        schema (load-schema)
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        context {:sqs sqs
                 :workers-queue-url workers-queue-url}
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
