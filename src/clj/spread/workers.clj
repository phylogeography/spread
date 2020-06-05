(ns spread.workers
  (:require [clojure.core.async :refer [close! thread]]
            [mount.core :as mount :refer [defstate]]
            [spread.config :as config]
            [spread.messaging :as messaging]
            [taoensso.timbre :as log]))

(defn create-worker
  "listens on the queue and executes tasks as they arrive"
  [id queue-name]
  (thread
    (messaging/create-listener queue-name
                               (fn [channel meta message-type body]

                                 (log/debug "worker received message" {:worker-id id
                                                                       ;; :channel channel
                                                                       ;; :meta meta
                                                                       ;; :message-type message-type
                                                                       :body body})

                                 ))))

(defn stop [all-workers]
  (doall (for [worker all-workers]
           (close! worker))))

(defn start []
  (let [{:keys [workers-count queue-name]} (:workers config/config)]
    (doall (for [id (range 0 workers-count)]
             (create-worker id queue-name)))))

(defstate workers
  :start (start)
  :stop (stop workers))
