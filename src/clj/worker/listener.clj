(ns worker.listener
  (:require
   [aws.sqs :as aws-sqs]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   )
  (:import (io.nodrama ContinuousTreeParser)) )

;; TODO : invoke java parser, process message body
(defn handle-message [body]
  (log/info "Handling message" {:msg body} )
  (Thread/sleep (or (:sleep body) 3000)) ;; long process
  )

(defn start [{:keys [aws] :as config}]
  (let [{:keys [workers-queue-url]} aws
        sqs (aws-sqs/create-client aws)
        parser (new ContinuousTreeParser)]
    (log/info "Starting worker listener" {:class parser})

    (.parseTree parser)

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
