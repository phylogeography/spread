(ns ui.home.events
  (:require [re-frame.core :as re-frame]
            [ui.websocket-fx :as websocket]
            [ui.graphql :as graphql]
            [taoensso.timbre :as log]))

(re-frame/reg-event-fx
  ::initialize-page
  (fn [{:keys [db]}]
    {:forward-events {:register    :websocket-athorized?
                      :events      #{::graphql/ws-authorized}
                      :dispatch-to [::initial-query]}}))

(re-frame/reg-event-fx
  ::initial-query
  (fn [{:keys [db]}]
    {:dispatch-n [[::graphql/query {:query
                                    "query {
                                       getAuthorizedUser {
                                         id
                                         email
                                       }
                                     }"}]
                  ;; TODO : this is for POC only, subscribe to status=QUEUED/RUNNING analysis only
                  [::graphql/subscription {:id        :home-page
                                           :query     "subscription SubscriptionRoot($id: ID!) {
                                                         discreteTreeParserStatus(id: $id) {
                                                           id
                                                           status
                                                        }
                                                      }"
                                           :variables {"id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}}]]}))

(comment
  (re-frame/reg-event-fx
    ::on-message
    (fn [{:keys [db]} [_ message]]
      (log/debug "home/on-message" message)))

  (re-frame/dispatch [::websocket/subscribe :default
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
