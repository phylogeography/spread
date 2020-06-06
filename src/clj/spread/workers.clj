(ns spread.workers
  (:require [clojure.core.async :refer [close! thread >!! <!!] :as async]
            [mount.core :as mount :refer [defstate]]
            [spread.config :as config]
            [spread.messaging :as messaging]
            [spread.logging :as logging]
            [taoensso.timbre :as log]

            [langohr.basic :as basic]
            [langohr.channel :as channel]
            [langohr.confirm :as confirm]
            [langohr.consumers :as consumers]
            [langohr.core :as langohr]
            [langohr.queue :as queue]))

(defn create-worker
  "listens on the queue and executes tasks as they arrive"
  [id queue-name]
  (thread
    (log/info (format "Creating worker with id %d for queue %s" id queue-name) {})
    (let [conn (langohr/connect)
          sync-ch (async/chan)
          queue-ch (messaging/create-listener conn
                                              queue-name
                                              (fn [& args] (>!! sync-ch args))
                                              ;; this is to create a fair dispatch see: https://www.rabbitmq.com/tutorials/tutorial-two-java.html
                                              {:qos 1})]

      (loop []
        ;; check that we didn't get a message telling us to shutdown
        (let [[_ meta message-type body] (<!! sync-ch)]

          (log/debug (format "Worker %d [%d] received message (type = %s) %s" id (.getId (Thread/currentThread)) message-type body) {})

          (Thread/sleep (:sleep body)) ;; long process

          (log/debug (format "Worker %d finished processing message" id) {})

          (recur))))))


(defn stop [all-workers]
  (doall (for [worker all-workers]
           (close! worker))))

(defn start []
  (let [{:keys [workers-count queue-name ]} (:workers config/config)
        new-worker-id (-> (mount/args) :workers :new-worker-id)]
    {:workers-threads (if new-worker-id

                        [(create-worker new-worker-id queue-name)]

                        (doall (for [id (range 0 workers-count)]
                                 (create-worker id queue-name))))}))

(defstate workers
  :start (start)
  :stop (stop workers))

(defn -main [[new-worker-id & args]]
  (log/info (format "Starting new worker with id: %s" new-worker-id) {})
  (-> (mount/only #{#'logging/logging
                    #'config/config
                    #'spread.workers/workers})
      (mount/with-args {:config "./config/config.dev.edn"
                        :workers {:new-worker-id (Integer/parseInt (str new-worker-id))}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config config/config}))))
