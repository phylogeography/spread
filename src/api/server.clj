(ns api.server
  (:require
   [io.pedestal.http :as http]
   [com.walmartlabs.lacinia.pedestal2 :as pedestal]
   [com.walmartlabs.lacinia.schema :as schema]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   ))

(def schema
  (schema/compile
   {:queries
    {:hello
     ;; String is quoted here; in EDN the quotation is not required
     {:type 'String
      :resolve (constantly "world")}}}))

(defn stop [this]
  (.stop this))

(defn start [config]
  (let [{:keys [port] :as args} (:api config)
        service (pedestal/default-service schema {:port (Integer/parseInt port)})
        runnable-service (http/create-server service)]
    (log/info "Starting server" args)
    (http/start runnable-service)
    runnable-service))

(defstate server
  :start (start (mount/args))
  :stop (stop server))
