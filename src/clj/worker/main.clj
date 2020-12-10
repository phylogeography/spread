(ns worker.main
  (:gen-class)
  (:require [mount.core :as mount]
            [shared.logging :as logging]
            [taoensso.timbre :as log]
            [worker.config :as config]
            [worker.listener :as listener]))

(defn start []
  (let [config (config/load)]
    (-> (mount/only #{#'logging/logging
                      #'listener/listener})
        (mount/with-args config)
        (mount/start)
        (as-> $ (log/warn "Started worker" {:components $
                                            :config config})))))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& _]
  (start))
