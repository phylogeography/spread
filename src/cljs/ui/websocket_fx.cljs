(ns ui.websocket-fx
  (:require [cljs.core.async :as async]
            [clojure.string :as strings]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.ws-client :as ws-client]))

;; NOTE: adapted from https://github.com/RutledgePaulV/websocket-fx/blob/develop/src/websocket_fx/core.cljs

(defonce CONNECTIONS (atom {}))

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

;; FX HANDLERS

(re-frame/reg-fx
  ::connect
  (fn [{socket-id
        :socket-id
        {:keys [url protocols on-connect on-disconnect]
         :or   {url (websocket-url)}}
        :options}]
    (let [sink-proxy (async/chan 100)]
      (swap! CONNECTIONS assoc socket-id {:sink sink-proxy})
      (async/go
        (let [{:keys [socket source sink close-status]}
              (async/<! (ws-client/connect url {:protocols protocols}))
              mult (async/mult source)]
          (swap! CONNECTIONS assoc socket-id {:sink sink-proxy :source source :socket socket})
          (async/go
            (when-some [closed (async/<! close-status)]
              (re-frame/dispatch [:websocket/disconnected socket-id closed])
              (when (some? on-disconnect) (re-frame/dispatch on-disconnect))))
          (when-not (async/poll! close-status)
            (async/go-loop []
              (when-some [{:keys [id proto data close timeout]
                           :or   {timeout 10000}} (async/<! sink-proxy)]

                (cond
                  (#{:request} proto)
                  (let [xform         (filter (fn [msg]
                                                (or
                                                  ;; NOTE : lacinia does not return id with the connection_init so we must assume that this is the response to the last sent ws request
                                                  (= (:type msg) "connection_ack")
                                                  (= (:id msg) id))))
                        response-chan (async/tap mult (async/chan 1 xform))
                        timeout-chan  (async/timeout timeout)]
                    (async/go
                      (let [[value _] (async/alts! [timeout-chan response-chan])]
                        (if (some? value)
                          (re-frame/dispatch [:websocket/request-response socket-id id value])
                          (re-frame/dispatch [:websocket/request-timeout socket-id id :timeout])))))

                  (#{:subscription} proto)
                  (let [xform         (filter (fn [msg]
                                                (= (:id msg) id)))
                        response-chan (async/tap mult (async/chan 1 xform))]
                    (async/go-loop []
                      (when-some [{:keys [close] :as response} (async/<! response-chan)]
                        (if (true? close)
                          (do (async/close! response-chan)
                              (re-frame/dispatch [:websocket/subscription-closed socket-id id]))
                          (do (re-frame/dispatch [:websocket/subscription-message socket-id id response])
                              (recur)))))))

                (when (if (some? close)
                        (async/>! sink {:id id :proto proto :close close})
                        (async/>! sink (merge data {:id id :proto proto})))
                  (recur))))
            (re-frame/dispatch [:websocket/connected socket-id])
            (when (some? on-connect) (re-frame/dispatch on-connect))))))))

(re-frame/reg-fx
  ::disconnect
  (fn [{:keys [socket-id]}]
    (let [{:keys [socket]} (get (first (swap-vals! CONNECTIONS dissoc socket-id)) socket-id)]
      (when (some? socket) (.close socket)))))

(re-frame/reg-fx
  ::ws-message
  (fn [{:keys [socket-id message]}]
    (if-some [{:keys [sink]} (get @CONNECTIONS socket-id)]
      (async/put! sink message)
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
