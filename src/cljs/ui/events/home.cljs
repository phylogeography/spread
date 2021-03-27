(ns ui.events.home
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.home.page :refer [analysis-id]]))

(defn initialize-page [_]
  {:forward-events {:register    :websocket-authorized?
                    :events      #{:graphql/ws-authorized}
                    :dispatch-to [:home/initial-query]}})

(defn initialize-query [_]
  {:dispatch-n [[:graphql/query {:query
                                 "query {
                                       getAuthorizedUser {
                                         id
                                         email
                                       }
                                     }"}]
                ;; TODO : this is for POC only, subscribe to status=QUEUED/RUNNING analysis only
                [:graphql/subscription {:id        :home-page
                                        :query     "subscription SubscriptionRoot($id: ID!) {
                                                         discreteTreeParserStatus(id: $id) {
                                                           id
                                                           status
                                                        }
                                                      }"
                                        :variables {"id" analysis-id}}]]})

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
