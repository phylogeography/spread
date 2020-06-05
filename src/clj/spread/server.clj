(ns spread.server
  (:require [mount.core :as mount :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [spread.config :as config]
            [spread.routing :as routing]))

(defn stop [this]
  (.stop this))

(defn start []
  (let [{:keys [host port queue-name] :as args} (:api config/config)]
    (jetty/run-jetty (routing/create-handler {:context {:queue-name queue-name}}) {:host host :port port :join? false})))

(defstate server
  :start (start)
  :stop (stop server))
