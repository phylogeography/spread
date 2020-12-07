(ns api.main
  (:gen-class)
  (:require [mount.core :as mount]
            [api.config :as config]
            [shared.logging :as logging]
            [api.server :as server]
            [taoensso.timbre :as log]))

(defn start []
  (let [config (config/load)]
    (-> (mount/only #{#'logging/logging
                      #'server/server})
        (mount/with-args config)
        (mount/start)
        (as-> $ (log/warn "Started" {:components $
                                     :config config})))))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn -main []
  (start))
