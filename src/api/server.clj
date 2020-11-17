(ns api.server
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia.pedestal2 :as pedestal]
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util :as lacinia-util]
   [io.pedestal.http :as http]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   ))

(defn get_parser_execution
  [_ {:keys [id] :as args} _]
  (log/debug "get_parser_execution" {:a args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :SUCCEEDED
   :output "s3://spread-dev-uploads/4d07edcf-4b4b-4190-8cea-38daece8d4aa"})

;; TODO : publish worker message
(defn start_parser_execution
  [context args value]
  (log/debug "start_parser_execution" {:a args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :RUNNING})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn resolver-map []
  {:query/get_parser_execution get_parser_execution
   :mutation/start_parser_execution start_parser_execution})

(defn stop [this]
  (http/stop this))

(defn start [config]
  (let [{:keys [port] :as args} (:api config)
        schema (load-schema)
        compiled-schema (-> schema
                            (lacinia-util/attach-resolvers (resolver-map))
                            schema/compile)
        service (pedestal/default-service compiled-schema {:port (Integer/parseInt port)})
        runnable-service (http/create-server service)]
    (log/info "Starting server" {:a args :r runnable-service})
    (http/start runnable-service)
    runnable-service))

(defstate server
  :start (start (mount/args))
  :stop (stop server))
