(ns ui.home.events
  (:require [re-frame.core :as re-frame]
            [ui.websocket-fx :as websocket]
            [taoensso.timbre :as log]))

;; TODO : dispatch initial query (user analysis etc)
(re-frame/reg-event-fx
  ::initialize-page
  (fn [{:keys [db]}]

    (log/debug "home/initialize-page" db)

    #_{:forward-events {:register    :websocket-connected?
                        :events      #{::websocket/connected}
                        :dispatch-to [::initial-query]}}

    #_{:dispatch [::websocket/subscribe :default {:message
                                                  {:id      "1"
                                                   :type    "start"
                                                   :payload {:variables     {"Id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}
                                                             :extensions    {}
                                                             :operationName nil
                                                             :query         "subscription SubscriptionRoot($Id: ID!) {
                                                                             discreteTreeParserStatus(id: $Id) {
                                                                               status
                                                                             }
                                                                           }"}}

                                                  :on-message [::on-message]
                                                  ;; :on-close   [::users-watch-closed]
                                                  }]}
    ))


(re-frame/reg-event-fx
  ::initial-query
  ;; ::initialize-page
  (fn [{:keys [db]}]

    (log/debug "home/initial-query" db)

    {:dispatch [::websocket/subscribe :default
                :home-page
                "fubar"
                #_{:message
                   {:id      "1"
                    :type    "start"
                    :payload {:variables     {"Id" "60b08880-03e6-4a3f-a170-29f3c75cb43f"}
                              :extensions    {}
                              :operationName nil
                              :query         "subscription SubscriptionRoot($Id: ID!) {
                                                                             discreteTreeParserStatus(id: $Id) {
                                                                               status
                                                                             }
                                                                           }"}}

                   :on-message [::on-message]
                   ;; :on-close   [::users-watch-closed]
                   }]}
    ))

(re-frame/reg-event-fx
  ::on-message
  (fn [{:keys [db]} [_ message]]

    (log/debug "home/on-message" message)

    ))


(comment

  (re-frame/dispatch [::websocket/subscribe :default
                      :home-page
                      {:message
                       {:id      "1"
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
