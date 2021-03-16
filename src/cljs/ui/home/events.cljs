(ns ui.home.events
  (:require [re-frame.core :as re-frame]
            [ui.websocket-fx :as websocket]
            [ui.graphql :as graphql]
            [taoensso.timbre :as log]))

;; TODO : dispatch initial query (user analysis etc)
(re-frame/reg-event-fx
  ::initialize-page
  (fn [{:keys [db]}]
    ;; (log/debug "home/initialize-page" db)
    {:forward-events {:register    :websocket-athorized?
                      :events      #{::graphql/ws-authorized}
                      :dispatch-to [::initial-query]}}))


(re-frame/reg-event-fx
  ::initial-query
  (fn [{:keys [db]}]

    (log/debug "home/initial-query" db)

    {:dispatch [::graphql/subscription {:id    "home-page"
                                        :query "subscription SubscriptionRoot($id: ID!) {
                                                  discreteTreeParserStatus(id: $id) {
                                                    id
                                                    status
                                                  }
                                                }"
                                        :variables {"id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}}]}))

(re-frame/reg-event-fx
  ::on-message
  (fn [{:keys [db]} [_ message]]

    (log/debug "home/on-message" message)

    ))


(comment

  (re-frame/dispatch [::websocket/subscribe :default
                      "home-page"
                      {:message
                       {;;:id      "1"
                        :type    "start"
                        :payload {:variables     {"Id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}
                                  :extensions    {}
                                  :operationName nil
                                  :query         "subscription SubscriptionRoot($Id: ID!) {
                                                                                   discreteTreeParserStatus(id: $Id) {
                                                                                     status
                                                                                   }
                                                                                 }"}}
                       :on-message [:ui.home.events/on-message]
                       ;; :on-close   [::users-watch-closed]
                       }])






  )
