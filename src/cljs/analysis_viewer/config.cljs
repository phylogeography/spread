(ns analysis-viewer.config
  (:require [shared.macros :refer [get-env-variable]]))

(def environment (get-env-variable "SPREAD_ENV" :required))


(def default-config
  {:version "1.0.3"
   :environment environment
   :s3-bucket-url  "http://127.0.0.1:9000/spread-dev-uploads/"})

(def dev-config
  (-> default-config
      (assoc-in [:logging :level] :debug)))

(def prod-config
  (-> default-config
      (assoc :s3-bucket-url "https://spread-prod-uploads.s3.us-east-2.amazonaws.com/")))

(defn load []
  (case environment
    "dev"  dev-config
    "prod" prod-config
    dev-config))
