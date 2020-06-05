(ns spread.core
  (:gen-class)
  (:require [mount.core :as mount]
            [spread.config :as config]
            [spread.logging :as logging]
            [spread.messaging :as messaging]
            [spread.server :as server]
            [spread.workers :as workers]
            [taoensso.timbre :as log]))

(defn start [& [path]]
  (-> (mount/only #{#'logging/logging
                    #'server/server
                    #'messaging/messaging
                    #'workers/workers
                    #'config/config})
   (mount/with-args {:config (or path "./config/config.dev.edn")})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config config/config}))))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (let [path (first args)]
    (start path)))
