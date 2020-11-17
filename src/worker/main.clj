(ns worker.main
  (:require

   ;; [clojure.java.shell :refer [sh]]
   ;; [clojure.string :as str]
   ;; [taoensso.timbre :as log]
   ;; [flow-storm.api :as fsa]
   ;; [clojure.java.io :as io]
   [api.config :as config]
   ;; [api.db :as db]
   ;; [api.models.videos :as models.videos]
   ;; [api.aws.s3 :as aws-s3]
   ;; [aws.sqs :as aws-sqs]

   )
  (:gen-class))

(defn consume-and-process-message

  "Consume and process one message from the queue.
  Returns :ok on success or :empty if the queue was empty and we did nothing."

  [{:keys [sqs db transcoding-queue-url notification-queue-url] :as ctx}]
  (if-let [{:keys [body receipt-handle]} (aws-sqs/get-next-message sqs transcoding-queue-url)]
    (let [_ (log/info "Got a message" {:msg body})]

      ;; confirm we have processed the message successfully
      (aws-sqs/ack-message sqs transcoding-queue-url receipt-handle)

      :ok)

    :empty))

(defn -main [& _]

  (when (:flow-storm (config/config)) (fsa/connect))

  (log/info "Starting spread worker" {:config (config/config)})

  #_(let [{:keys [logging db aws] :as config} (config/config)
        {:keys [transcoding-queue-url notification-queue-url]} aws
        datasource (db/init db)
        sqs (aws-sqs/create-client aws)
        s3 (aws-s3/create-client aws)
        ctx {:db  datasource
             :sqs sqs
             :s3  s3
             :config config
             :transcoding-queue-url transcoding-queue-url
             :notification-queue-url notification-queue-url}]

    #_(log/merge-config! logging)

    #_(while true
      (try
        ;; If the queue is empty, wait for 2 seconds and poll again
        (when (= :empty (consume-and-process-message ctx))
          (Thread/sleep 2000))

        (catch Exception e
          (log/error "Error processing a message" {:error e}))
        (finally
          ;; clean everything, we don't need the files on disk anymore
          (clean-tmp-folder))))))
