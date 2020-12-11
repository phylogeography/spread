(ns api.main
  (:gen-class)
  (:require [api.config :as config]
            [api.server :as server]
            [mount.core :as mount]
            [shared.logging :as logging]
            [taoensso.timbre :as log]))

(defn start []
  (let [config (config/load!)]
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

(defn -main [& _]
  (start))
