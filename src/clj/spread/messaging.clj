(ns spread.messaging
  (:require [clojure.edn :as edn]
            [langohr.basic :as basic]
            [langohr.channel :as channel]
            [langohr.confirm :as confirm]
            [langohr.consumers :as consumers]
            [langohr.core :as langohr]
            [langohr.queue :as queue]
            [mount.core :as mount :refer [defstate]]
            [spread.config :as config]
            [taoensso.timbre :as log]))

(defn valid-message-type? [message-type]
  (and (keyword? message-type)
       (namespace message-type)
       (not (.contains (namespace message-type) "."))))

(defmulti encode (fn [content-type message-type message] content-type))

(defmethod encode "application/edn" [_ message-type message]
  (assert (valid-message-type? message-type) (str "cannot EDN encode message, message type " message-type " is not a valid message type"))
  (pr-str {:timestamp (System/currentTimeMillis)
           :type message-type
           :body message}))

(defmulti decode (fn [content-type payload] content-type))

(defmethod decode :default [content-type _]
  (throw (Exception. (str "no decoder for content type " content-type))))

(defmethod decode "application/edn" [_ bytes]
  (let [message (edn/read-string {:default (fn [tag data] (str tag data))} (String. bytes java.nio.charset.StandardCharsets/UTF_8))
        {:keys [type body]} message]
    (when-not type
      (ex-info {:type ::missing-message-type
                :message message}))
    [(keyword type) body]))

(defn create-connection
  ([]
   (create-connection (:messaging config/config)))
  ([config]
   (langohr/connect config)))

(def ^:dynamic *connection*
  "Unique RabbitMQ connection"
  (delay (create-connection)))

(defn default-return-listener [reply-code reply-text exchange routing-key properties body]
  (log/error "Could not send execution response" {:queue routing-key
                                                  :repl-text reply-text
                                                  :reply-code reply-code}))

(defn open-channel
  "Open a channel using the application unique broker connection or one provided.
  A return listener function can be provided for specific behavior in case of failure

  In most cases this should only be used to publish messages directly without using rpc
  Listeners should be created using the make-queue-listener function"
  ([]
   (open-channel @*connection*))
  ([conn]
   (open-channel conn default-return-listener))
  ([conn return-listener]
   (doto (channel/open conn)
     (basic/add-return-listener return-listener)
     (confirm/select))))

(defn ensure-queue [channel queue-name]
  (queue/declare channel queue-name {:durable true
                                     :auto-delete false}))

(defn close-connection
  []
  (log/info "Closing broker connection")
  (.close @*connection*))

(defn publish
  "Publish a message to a channel with application/edn content-type"
  ([channel {:keys [topic routing-key queue] :as route-info} message-type message]
   (publish channel route-info message-type message {}))
  ([channel
    {:keys [topic routing-key queue]}
    message-type message
    {:keys [content-type wait-for-confirms?]
     :or {content-type "application/edn"
          wait-for-confirms? true}
     :as options}]
   (assert (or (and (and topic routing-key) (not queue))
               (and queue (not (or topic routing-key))))
           (str "either :queue or (:topic and :routing-key) need to be specified, "
                "but not both or :topic alone or :routing-key alone"))
   (basic/publish channel
                  (or topic "")
                  (or queue routing-key)
                  (encode content-type message-type message)
                  (into {:content-type content-type
                         :persistent true
                         :headers (let [now (java.time.Instant/now)]
                                    {"process" (-> (java.lang.management.ManagementFactory/getRuntimeMXBean) .getName)
                                     "user" (System/getenv "USER")
                                     "epoch-seconds" (.getEpochSecond now)
                                     "nanoseconds" (.getNano now)})}
                        options))
   (when wait-for-confirms?
     (confirm/wait-for-confirms channel))))

(defn handle-message [{:keys [no-ack?]}
                      handler
                      channel
                      {:keys [content-type delivery-tag message-id reply-to]
                       :or {content-type "application/edn"}
                       :as meta}
                      payload]
  (let [[message-type body] (try
                              (decode content-type payload)
                              (catch Exception e
                                (log/error "Cannot decode message" {:error e})))]
    (try
      (handler channel meta message-type body)
      (when-not no-ack?
        (basic/ack channel delivery-tag))
      (catch Exception e
        (log/error "Cannot process message" {:error e})))))

(defn create-listener
  "Opens a channel and attaches a handler to the queue
  Handler is a function with following signature:
  fn [channel meta message-type body]
  Each listener opens it's own channel because channel instances must not be shared between threads"
  ([conn queue handler]
   (create-listener conn queue handler {}))
  ([conn queue handler {:keys [return-listener no-ack? transient? qos] :as options :or {qos 10}}]
   (assert (string? queue))
   (let [channel (open-channel conn (or return-listener default-return-listener))]
     (when qos
       (basic/qos channel qos))
     (let [queue (if transient?
                   (:queue (queue/declare channel queue {:durable false :exclusive true :auto-delete true}))
                   queue)
           consumer-tag (consumers/subscribe channel queue (partial handle-message options handler))]
       (log/info "Started listening" {:queue queue
                                      :consumer-tag consumer-tag}))
     channel)))

(defn start []
  (let [{:keys [queues]} (:messaging config/config)]
    (with-open [channel (open-channel)]
      (doseq [queue queues]
        (ensure-queue channel queue)))))

(defstate messaging
  :start (start)
  :stop (close-connection))
