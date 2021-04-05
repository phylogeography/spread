(ns ui.events.websocket
  (:require [ui.utils :refer [concatv dissoc-in]]))

(defn connect [{:keys [db]} [_ socket-id command]]
  (let [data {:status :pending :options command}]
    {:db                      (assoc-in db [::sockets socket-id] data)
     :ui.websocket-fx/connect {:socket-id socket-id :options command}}))

(defn disconnect [{:keys [db]} [_ socket-id]]
  {:db                         (dissoc-in db [::sockets socket-id])
   :ui.websocket-fx/disconnect {:socket-id socket-id}})

(defn connected [{:keys [db]} [_ socket-id]]
  {:db
   (assoc-in db [::sockets socket-id :status] :connected)
   :dispatch-n
   (vec (for [sub (vals (get-in db [::sockets socket-id :subscriptions] {}))]
          [:websocket/subscribe socket-id (get sub :id) sub]))})

;; TODO : exponential backoff
(defn disconnected [{:keys [db]} [_ socket-id cause]]
  (let [options (get-in db [::sockets socket-id :options])]
    {:db
     (assoc-in db [::sockets socket-id :status] :reconnecting)
     :dispatch-n
     (vec (for [request-id (keys (get-in db [::sockets socket-id :requests] {}))]
            [:websocket/request-timeout socket-id request-id cause]))
     :dispatch-later
     [{:ms 2000 :dispatch [:websocket/connect socket-id options]}]}))

(defn request [{:keys [db]} [_ socket-id {:keys [message timeout] :as command}]]
  (let [payload (cond-> {:id (random-uuid) :proto :request :data message}
                  (some? timeout) (assoc :timeout timeout))
        path    [::sockets socket-id :requests (get payload :id)]]
    {:db                         (assoc-in db path command)
     :ui.websocket-fx/ws-message {:socket-id socket-id :message payload}}))

(defn request-response [{:keys [db]} [_ socket-id request-id & more]]
  (let [path    [::sockets socket-id :requests request-id]
        request (get-in db path)]
    (cond-> {:db (dissoc-in db path)}
      (contains? request :on-response)
      (assoc :dispatch (concatv (:on-response request) more)))))

(defn request-timeout [{:keys [db]} [_ socket-id request-id & more]]
  (let [path    [::sockets socket-id :requests request-id]
        request (get-in db path)]
    (cond-> {:db (dissoc-in db path)}
      (contains? request :on-timeout)
      (assoc :dispatch (concatv (:on-timeout request) more)))))

(defn subscribe [{:keys [db]} [_ socket-id topic {:keys [message] :as command}]]
  (let [path    [::sockets socket-id :subscriptions topic]
        payload {:id topic :proto :subscription :data message}]
    {:db                         (assoc-in db path command)
     :ui.websocket-fx/ws-message {:socket-id socket-id :message payload}}))

(defn subscription-message [{:keys [db]} [_ socket-id subscription-id & more]]
  (let [path         [::sockets socket-id :subscriptions subscription-id]
        subscription (get-in db path)]
    (cond-> {}
      (contains? subscription :on-message)
      (assoc :dispatch (concatv (:on-message subscription) more)))))

(defn unsubscribe [{:keys [db]} [_ socket-id subscription-id & more]]
  (let [path         [::sockets socket-id :subscriptions subscription-id]
        payload      {:id subscription-id :proto :subscription :close true}
        subscription (get-in db path)]
    (cond-> {:db (dissoc-in db path)}
      (some? subscription)
      (assoc :ui.websocket-fx/ws-message {:socket-id socket-id :message payload})
      (contains? subscription :on-close)
      (assoc :dispatch (concatv (:on-close subscription) more)))))

(defn subscription-closed [{:keys [db]} [_ socket-id subscription-id & more]]
  (let [path [::sockets socket-id :subscriptions subscription-id]]
    (if-some [subscription (get-in db path)]
      (cond-> {:db (dissoc-in db path)}
        (contains? subscription :on-close)
        (assoc :dispatch (concatv (:on-close subscription) more))))))

(defn push [_ [_ socket-id command]]
  (let [payload {:id (random-uuid) :proto :push :data command}]
    {:ui.websocket-fx/ws-message {:socket-id socket-id :message payload}}))
