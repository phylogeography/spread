(ns ui.events.general
  (:require [taoensso.timbre :as log]
            [ui.router.queries :as router-queries]))

(def socket-id :default)

(defn active-page-changed [{:keys [db]}]
  (let [{:keys [name] :as active-page} (router-queries/active-page db)]
    (log/info "Active page changed" active-page)
    (case name
      :route/splash {:dispatch [:splash/initialize-page]}
      :route/home   {:dispatch [:home/initialize-page]}
      nil)))

(defn logout [{:keys [localstorage]}]
  {:localstorage (dissoc localstorage :access-token)
   :dispatch     [:router/navigate :route/splash]})

;; TODO : initial graphql queries to fill the data in left pane menu:
;; - completed analysis query (paginated)
;; - RUNNING analysis query (paginated)
;; - create subscriptions to RUNNING analysis statuses
;; - close subscriptions when status changes

(defn initialize [{:keys [db]} [_ config]]
  (let [{:keys [name] :as active-page} (router-queries/active-page db)]
    {:db         (assoc db :config config)
     :dispatch-n [[:websocket/connect socket-id {:url        (-> config :graphql :ws-url)
                                                 :format     :json
                                                 :on-connect [:graphql/ws-authorize
                                                              {:on-timeout [:graphql/ws-authorize-failed]}]
                                                 :protocols  ["graphql-ws"]}]

                  [:graphql/query {:query
                                   "query {
                                       getAuthorizedUser {
                                         id
                                         email
                                       }
                                     }"}]]
     :forward-events {:register    :active-page-changed
                      :events      #{:router/active-page-changed}
                      :dispatch-to [:general/active-page-changed]}}))
