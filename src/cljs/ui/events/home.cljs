(ns ui.events.home
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

(defn initialize-page [_]
  {:forward-events {:register    :websocket-authorized?
                    :events      #{:graphql/ws-authorized}
                    :dispatch-to [:home/initial-query]}})

;; TODO: this should be probably done on every page
;; since user can start from a non-home page
;; perhaps even moved to the initialize flow?
(defn initial-query
  "if user opens home page we subscribe to all ongoing analysis"
  [{:keys [db]}]
  (let [queued (->> (db :user-analysis :analysis)
                    (filter #(#{"QUEUED" "RUNNING"} (:status %)))
                    (#(map :id %)))]
    {:dispatch-n [(for [id queued]
                    [:graphql/subscription {:id        id
                                            :query     "subscription SubscriptionRoot($id: ID!) {
                                                           parserStatus(id: $id) {
                                                             id
                                                             status
                                                             progress
                                                             ofType
                                                           }}"
                                            :variables {:id id}}])]}))

(comment
  (re-frame/reg-event-fx
    ::on-message
    (fn [_ [_ message]]
      (log/debug "home/on-message" message)))
  (re-frame/dispatch [:websocket/subscribe :default
                      "home-page"
                      {:message
                       {:id      "home-page"
                        :type    "start"
                        :payload {:variables     {"Id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}
                                  :extensions    {}
                                  :operationName nil
                                  :query         "subscription SubscriptionRoot($Id: ID!) {
                                                                                   discreteTreeParserStatus(id: $Id) {
                                                                                     status
                                                                                   }
                                                                                 }"}}
                       :on-message [:ui.home.events/on-message]}]))
