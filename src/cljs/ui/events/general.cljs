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
  {:db         (assoc db :config config)
   ;; TODO : only if there is token in localstorage, else it will result in an auth error
   :dispatch-n [[:websocket/connect socket-id {:url        (-> config :graphql :ws-url)
                                               :format     :json
                                               :on-connect [:graphql/ws-authorize
                                                            {:on-timeout [:graphql/ws-authorize-failed]}]
                                               :protocols  ["graphql-ws"]}]
                ;; NOTE : to avoid duplications during dev
                (when-not (:user-analysis db)
                  [:graphql/query {:query
                                   "query SearchAnalysis {
                                      getAuthorizedUser {
                                        id
                                        email
                                      }
                                      searchUserAnalysis(first: 5, statuses: [SUCCEEDED]) {
                                        pageInfo {
                                          hasNextPage
                                          startCursor
                                          endCursor
                                        }
                                        edges {
                                          cursor
                                          node {
                                            id
                                            readableName
                                            ofType
                                            status
                                            createdOn
                                          }
                                        }
                                      }
                                    }"}])]
   :forward-events {:register    :active-page-changed
                    :events      #{:router/active-page-changed}
                    :dispatch-to [:general/active-page-changed]}})
