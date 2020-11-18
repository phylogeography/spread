(ns api.config
  (:require [shared.utils :refer [get-env-variable]]))

(defn load []
  (let [environment (or (get-env-variable "SPREAD_ENV") "dev")
        dev-env? (= "dev" environment)]
    {
     :logging {:level (or (keyword (get-env-variable "LOGGING_LEVEL")) :debug)}

     :api {:port (or (get-env-variable "API_PORT") "3001")}

     ;; :aws (cond-> {:region (get-env-variable "AWS_REGION" :required)
     ;;               :endpoint (get-env-variable "S3_ENDPOINT")
     ;;               :access-key-id  (get-env-variable "AWS_ACCESS_KEY_ID" :required)
     ;;               :secret-access-key  (get-env-variable "AWS_SECRET_ACCESS_KEY" :required)
     ;;               :bucket-name (get-env-variable "BUCKET_NAME" :required)
     ;;               :workers-queue-url (get-env-variable "WORKERS_QUEUE_URL" :required)}

     ;;        dev-env? (assoc :sqs-host "localhost" ;; TODO: maybe grab this from one of the urls
     ;;                        :sqs-port 9324)
     ;;        )

     ;; :db {:dbname (get-env-variable "DB_DATABASE" :required)
     ;;      :port (get-env-variable "DB_PORT" :required)
     ;;      :user (get-env-variable "DB_USER" :required)
     ;;      :password  (get-env-variable "DB_PASSWORD" :required)
     ;;      :host (get-env-variable "DB_HOST" :required)}

     }))
