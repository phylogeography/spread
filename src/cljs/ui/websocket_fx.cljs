(ns ui.websocket-fx
  (:require [cljs.core.async :as async]
            [clojure.string :as strings]
            [ui.ws-client :as ws-client]
            ;; [haslett.client :as ws-client]
            [haslett.format :as formats]
            [ui.utils :refer [dissoc-in concatv]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

;; NOTE: adapted from https://github.com/RutledgePaulV/websocket-fx/blob/develop/src/websocket_fx/core.cljs

(defonce CONNECTIONS (atom {}))

(def keyword->format
  {:edn                  formats/edn
   :json                 formats/json
   :transit-json         formats/transit
   :transit-json-verbose formats/transit})

(defn get-websocket-port []
  (str (aget js/window "location" "port")))

(defn get-websocket-host []
  (str (aget js/window "location" "hostname")))

(defn get-websocket-proto []
  (let [proto (str (aget js/window "location" "protocol"))]
    (get {"http:" "ws" "https:" "wss"} proto)))

(defn websocket-url []
  (let [proto (get-websocket-proto)
        host  (get-websocket-host)
        port  (get-websocket-port)
        path  "/ws"]
    (if (strings/blank? port)
      (str proto "://" host path)
      (str proto "://" host ":" port path))))

;; SOCKETS

(re-frame/reg-event-fx ::connect
                       (fn [{:keys [db]} [_ socket-id command]]
                         (let [data {:status :pending :options command}]
                           {:db       (assoc-in db [::sockets socket-id] data)
                            ::connect {:socket-id socket-id :options command}})))

(re-frame/reg-event-fx ::disconnect
                       (fn [{:keys [db]} [_ socket-id]]
                         {:db          (dissoc-in db [::sockets socket-id])
                          ::disconnect {:socket-id socket-id}}))

(re-frame/reg-event-fx ::connected
                       (fn [{:keys [db]} [_ socket-id]]

                         ;; (log/debug "@ websocket/connected" (get-in db [::sockets socket-id :subscriptions] ))

                         {:db
                          (assoc-in db [::sockets socket-id :status] :connected)
                          :dispatch-n
                          (vec (for [sub (vals (get-in db [::sockets socket-id :subscriptions] {}))]
                                 [::subscribe socket-id (get sub :id) sub]))}))

(re-frame/reg-event-fx ::disconnected
                       (fn [{:keys [db]} [_ socket-id cause]]
                         (let [options (get-in db [::sockets socket-id :options])]
                           {:db
                            (assoc-in db [::sockets socket-id :status] :reconnecting)
                            :dispatch-n
                            (vec (for [request-id (keys (get-in db [::sockets socket-id :requests] {}))]
                                   [::request-timeout socket-id request-id cause]))
                            :dispatch-later
                            [{:ms 2000 :dispatch [::connect socket-id options]}]})))

;; REQUESTS

(re-frame/reg-event-fx ::request
                       (fn [{:keys [db]} [_ socket-id {:keys [message timeout] :as command}]]
                         (let [payload (cond-> {:id (random-uuid) :proto :request :data message}
                                         (some? timeout) (assoc :timeout timeout))
                               path    [::sockets socket-id :requests (get payload :id)]]

                           (log/debug "request" payload)

                           {:db          (assoc-in db path command)
                            ::ws-message {:socket-id socket-id :message payload}})))

(re-frame/reg-event-fx ::request-response
                       (fn [{:keys [db]} [_ socket-id request-id & more]]
                         (let [path    [::sockets socket-id :requests request-id]
                               request (get-in db path)]

                           ;; (log/debug "@@@ request-response" more)

                           (cond-> {:db (dissoc-in db path)}
                             (contains? request :on-response)
                             (assoc :dispatch (concatv (:on-response request) more))))))

(re-frame/reg-event-fx ::request-timeout
                       (fn [{:keys [db]} [_ socket-id request-id & more]]
                         (let [path    [::sockets socket-id :requests request-id]
                               request (get-in db path)]
                           (cond-> {:db (dissoc-in db path)}
                             (contains? request :on-timeout)
                             (assoc :dispatch (concatv (:on-timeout request) more))))))

;; SUBSCRIPTIONS

(re-frame/reg-event-fx ::subscribe
                       (fn [{:keys [db]} [_ socket-id topic {:keys [message] :as command}]]

                         (log/debug "@ websocket/subscribe" {:id    socket-id
                                                             :topic topic
                                                             :cmd   command})

                         (let [path    [::sockets socket-id :subscriptions topic]
                               payload {:id topic :proto :subscription :data message}]
                           {:db          (assoc-in db path command)
                            ::ws-message {:socket-id socket-id :message payload}})))

(re-frame/reg-event-fx ::subscription-message
                       (fn [{:keys [db]} [_ socket-id subscription-id & more]]
                         (let [path         [::sockets socket-id :subscriptions subscription-id]
                               subscription (get-in db path)]
                           (cond-> {}
                             (contains? subscription :on-message)
                             (assoc :dispatch (concatv (:on-message subscription) more))))))

(re-frame/reg-event-fx ::unsubscribe
                       (fn [{:keys [db]} [_ socket-id subscription-id & more]]
                         (let [path         [::sockets socket-id :subscriptions subscription-id]
                               payload      {:id subscription-id :proto :subscription :close true}
                               subscription (get-in db path)]
                           (cond-> {:db (dissoc-in db path)}
                             (some? subscription)
                             (assoc ::ws-message {:socket-id socket-id :message payload})
                             (contains? subscription :on-close)
                             (assoc :dispatch (concatv (:on-close subscription) more))))))

(re-frame/reg-event-fx ::subscription-closed
                       (fn [{:keys [db]} [_ socket-id subscription-id & more]]
                         (let [path [::sockets socket-id :subscriptions subscription-id]]
                           (if-some [subscription (get-in db path)]
                             (cond-> {:db (dissoc-in db path)}
                               (contains? subscription :on-close)
                               (assoc :dispatch (concatv (:on-close subscription) more)))))))

;; PUSH

(re-frame/reg-event-fx ::push
                       (fn [_ [_ socket-id command]]
                         (let [payload {:id (random-uuid) :proto :push :data command}]
                           {::ws-message {:socket-id socket-id :message payload}})))

;; FX HANDLERS

(re-frame/reg-fx
  ::connect
  (fn [{socket-id
        :socket-id
        {:keys [url format protocols on-connect on-disconnect]
         :or   {format :edn
                url    (websocket-url)}}
        :options}]
    (let [sink-proxy (async/chan 100)]
      (swap! CONNECTIONS assoc socket-id {:sink sink-proxy})
      (async/go
        (let [{:keys [socket source sink close-status]}
              (async/<! (ws-client/connect url {:protocols protocols
                                                :format    (keyword->format format)}))
              mult (async/mult source)]
          (swap! CONNECTIONS assoc socket-id {:sink sink-proxy :source source :socket socket})
          (async/go
            (when-some [closed (async/<! close-status)]
              (re-frame/dispatch [::disconnected socket-id closed])
              (when (some? on-disconnect) (re-frame/dispatch on-disconnect))))
          (when-not (async/poll! close-status)
            (async/go-loop []
              (when-some [{:keys [id proto data close timeout] :or {timeout 10000} :as request} (async/<! sink-proxy)]

                (log/debug "go-loop/req" request)

                (cond
                  (#{:request} proto)
                  (let [xform         (filter (fn [msg]

                                                (log/debug "xform" msg)

                                                (or
                                                  (= (get msg "type") "connection_ack")
                                                  (= (:id msg) id))))
                        response-chan (async/tap mult (async/chan 1 xform))
                        timeout-chan  (async/timeout timeout)]
                    (async/go
                      (let [[value _] (async/alts! [timeout-chan response-chan])]
                        (log/debug "go-loop/req-resp" {:id id :value value})
                        (if (some? value)
                          (re-frame/dispatch [::request-response socket-id id (:data value)])
                          (re-frame/dispatch [::request-timeout socket-id id :timeout])))))

                  (#{:subscription} proto)
                  (let [xform         (filter (fn [msg] (= (:id msg) id)))
                        response-chan (async/tap mult (async/chan 1 xform))]
                    (async/go-loop []
                      (when-some [{:keys [data close] :as response} (async/<! response-chan)]

                        (log/debug "go-loop/sub-resp" response)

                        (if (true? close)
                          (do (async/close! response-chan)
                              (re-frame/dispatch [::subscription-closed socket-id id]))
                          (do (re-frame/dispatch [::subscription-message socket-id id data])
                              (recur)))))))
                (when (if (some? close)
                        (async/>! sink {:id id :proto proto :close close})
                        (do

                          (log/debug "go-loop/sink" data)

                          (async/>! sink (merge data {:id id :proto proto
                                                      ;; :data data
                                                      }))))
                  (recur))))
            (re-frame/dispatch [::connected socket-id])
            (when (some? on-connect) (re-frame/dispatch on-connect))))))))

(re-frame/reg-fx
  ::disconnect
  (fn [{:keys [socket-id]}]
    (let [{:keys [socket]} (get (first (swap-vals! CONNECTIONS dissoc socket-id)) socket-id)]
      (when (some? socket) (.close socket)))))

(re-frame/reg-fx
  ::ws-message
  (fn [{:keys [socket-id message]}]

    (log/debug "ws-message" message)

    (if-some [{:keys [sink]} (get @CONNECTIONS socket-id)]
      (async/put! sink
                  #_(merge message {:type    "connection_init"
                                  :payload {"Authorization"
                                            "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzcHJlYWQiLCJpYXQiOjEuNjE1Mjk2NzI1MzY5RTksImV4cCI6NC4yNDMyOTY3MjUzNjlFOSwiYXVkIjoic3ByZWFkLWNsaWVudCIsInN1YiI6ImExMTk1ODc0LTBiYmUtNGE4Yy05NmY1LTE0Y2RmOTA5N2UwMiJ9.ZdT-j8BJStTC4FZFawZPoZBXlHJ1AQc2A9T3xxzQYUdBntyCtxUPuKGBNyHLdJmfzdUm66LgVlZw1kiyXbh4xw"}})

                  message)
      (log/error (str "Socket with id " socket-id " does not exist.")))))

;; INTROSPECTION

(re-frame/reg-sub
  ::pending-requests
  (fn [db [_ socket-id]]
    (vals (get-in db [::sockets socket-id :requests]))))

(re-frame/reg-sub
  ::open-subscriptions
  (fn [db [_ socket-id]]
    (vals (get-in db [::sockets socket-id :subscriptions]))))

(re-frame/reg-sub
  ::status
  (fn [db [_ socket-id]]
    (get-in db [::sockets socket-id :status])))
