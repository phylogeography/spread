(ns aws.sqs
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [shared.utils :refer [decode-transit encode-transit]]
            [aws.utils :refer [throw-on-error]]))

(defn create-client

  "Creates a sqs client, needed to call all sqs related functions."

  [{:keys [access-key-id secret-access-key sqs-host sqs-port region]}]
  (aws/client (cond-> {:api :sqs
                       :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id access-key-id
                                               :secret-access-key secret-access-key})}
                region (assoc :region region)
                ;; only for dev
                sqs-host (assoc :endpoint-override {:protocol :http
                                                    :hostname sqs-host
                                                    :port sqs-port})

                ;; if we are in testing lets smash a real region
                ;; the aws/invoke fails with "No region found by any region provider."
                ;; if we don't provide a real one. Providing "spread-test-1" doesn't work
                sqs-host (assoc :region "us-east-1"))))

(defn stop-client [client]
  (aws/stop client))

(defn send-message

  "Puts a message in a aws sqs queue.
  Parameters description :

  sqs: the aws sqs client
  queue: a queue uir like \"http://127.0.0.1/queue/transcoding\"
  msg-body: any clojure datasctructure."

  [sqs queue msg-body]
  (-> (aws/invoke sqs {:op :SendMessage
                       :request {:QueueUrl queue
                                 :MessageBody (encode-transit msg-body)}})
      (throw-on-error {:api :sqs :fn ::send-message})))

(defn get-next-message

  "Gets the next message from a aws sqs queue.
  It doesn't delete the message, the message will remain in the queue
  invisible to others consumers until the visibility timeout period ends.
  Once you have process the message you can ack it using `api.aws.sqs/ack-message`.

  Parameters description :

  sqs: the aws sqs client
  queue: a queue url like \"http://127.0.0.1/queue/transcoding\"

  Returns a map with :

  :body - A clojure datastructure with the message content
  :receipt-handle - The handle you need for acking your message.

  Throws a Exception if something goes wrong when retrieving a message."

  [sqs queue]
  (let [{:keys [Messages]} (-> (aws/invoke sqs
                                           {:op :ReceiveMessage
                                            :request {:QueueUrl queue
                                                      :MaxNumberOfMessages 1
                                                      ;; should we configure this here or is it already configured
                                                      ;; in aws console?
                                                      ;; :VisibilityTimeout 100
                                                      }})
                               (throw-on-error {:api :sqs :fn ::get-next-message}))
        {:keys [Body ReceiptHandle]} (first Messages)]

    (when Body
      {:body (decode-transit Body)
       :receipt-handle ReceiptHandle})))

(defn ack-message

  "Confirms that your receipt-handle message was processed.
  It will be removed from the queue."

  [sqs queue receipt-handle]
  (-> (aws/invoke sqs {:op :DeleteMessage
                       :request {:QueueUrl queue
                                 :ReceiptHandle receipt-handle}})
      (throw-on-error {:api :sqs :fn ::ack-message})))

(comment
  (require '[transcoding.config :as config])

  (def sqs (create-client (-> (config) :aws)))

  (aws/invoke sqs {:op :ListQueues})
  (aws/doc sqs :SendMessage)
  (aws/doc test-sqs :ReceiveMessage)
  (aws/doc test-sqs :DeleteMessage)

  )
