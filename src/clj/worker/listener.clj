(ns worker.listener
  (:require
   [aws.sqs :as aws-sqs]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   )
  (:import (com.spread.parsers ContinuousTreeParser)) )

;; TODO : invoke java parser, process message body
(defn handle-message [body]
  (let [parser nil #_(doto (new ContinuousTreeParser)

                 (.setTreeFilePath "bla")


                 )]

    (log/info "Handling message" {:msg body
                                  :parser parser})

    ;; (.parseTree parser)

     ;; long process
    (Thread/sleep (or (:sleep body) 3000))

    ))

(defn start [{:keys [aws] :as config}]
  (let [{:keys [workers-queue-url]} aws
        sqs (aws-sqs/create-client aws)
        ]
    (log/info "Starting worker listener")

    (loop []
      (try
        ;; If the queue is empty, wait for 2 seconds and poll again
        ;; (log/debug "Polling...")
        (if-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs workers-queue-url)]
          (do
            (handle-message body)
            (aws-sqs/ack-message sqs workers-queue-url receipt-handle)))
        (Thread/sleep 2000)
        (catch Exception e
          (log/error "Error processing a message" {:error e})))
      (recur))))

(defstate listener
  :start (start (mount/args)))
