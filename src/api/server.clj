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

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string))

(defn resolver-map
  []
  {:query/game-by-id (fn [context args value]
                       nil)})

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
