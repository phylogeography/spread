(ns ui.events.general
  (:require [taoensso.timbre :as log]
            [ui.router.queries :as router-queries]))

(def socket-id :default)

(defn active-page-changed [{:keys [db]}]
  (let [{:keys [name] :as active-page} (router-queries/active-page db)]
    (log/debug "Active page changed" active-page)
    (case name
      :route/splash           {:dispatch [:splash/initialize-page]}
      :route/home             {:dispatch [:home/initialize-page]}
      :route/analysis-results {:dispatch [:analysis-results/initialize-page]}
      :route/new-analysis     {:dispatch [:new-analysis/initialize-page]}
      nil)))

(defn logout [{:keys [localstorage]}]
  {:localstorage (dissoc localstorage :access-token)
   :dispatch     [:router/navigate :route/splash]})

(def initial-user-data-query "query SearchAnalysis {
                                        getAuthorizedUser {
                                          id
                                          email
                                        }
                                        getUserAnalysis {
                                          id
                                          ofType
                                          readableName
                                          status
                                          error
                                          isNew
                                          createdOn
                                        }
                                      }")

(defn initialize [{:keys [db localstorage]} [_ config]]
  (let [{:keys [access-token]} localstorage]
    {:db             (assoc db :config config)
     :dispatch-n (cond-> [[:websocket/connect socket-id {:url        (-> config :graphql :ws-url)
                                                         :format     :json
                                                         :on-connect [:graphql/ws-authorize
                                                                      {:on-timeout [:graphql/ws-authorize-failed]}]
                                                         :protocols  ["graphql-ws"]}]]
                   ;; if we are authenticated we can retrieve user data
                   access-token (into [[:graphql/query {:query initial-user-data-query}]]))
     :forward-events {:register    :active-page-changed
                      :events      #{:router/active-page-changed}
                      :dispatch-to [:general/active-page-changed]}}))

(defn set-search [{:keys [db]} [_ text]]
  {:db (assoc db :search text)})

(defn query-analysis [_ [_ {:keys [id of-type]}]]
  {:dispatch [:graphql/query {:query (case (keyword of-type)
                                       :CONTINUOUS_TREE
                                       "query GetContinuousTree($id: ID!) {
                                          getContinuousTree(id: $id) {
                                            id
                                            readableName
                                            treeFileName
                                            attributeNames
                                            outputFileUrl
                                            xCoordinateAttributeName
                                            yCoordinateAttributeName
                                            mostRecentSamplingDate
                                            timeSlicer {
                                              id
                                              treesFileName
                                            }
                                            timescaleMultiplier
                                            analysis {
                                                viewerUrlParams
                                            }
                                          }
                                        }"

                                       :DISCRETE_TREE
                                       "query GetDiscreteTree($id: ID!) {
                                          getDiscreteTree(id: $id) {
                                            id
                                            readableName
                                            treeFileName
                                            locationsFileName
                                            attributeNames
                                            outputFileUrl
                                            locationsAttributeName
                                            mostRecentSamplingDate
                                            timescaleMultiplier
                                            analysis {
                                              viewerUrlParams
                                            }
                                          }
                                        }"

                                       :BAYES_FACTOR_ANALYSIS
                                       "query GetBayesFactorAnalysis($id: ID!) {
                                          getBayesFactorAnalysis(id: $id) {
                                            id
                                            readableName
                                            logFileName
                                            locationsFileName
                                            burnIn
                                            bayesFactors {
                                              from
                                              to
                                              bayesFactor
                                              posteriorProbability
                                            }
                                            outputFileUrl
                                            analysis {
                                              viewerUrlParams
                                            }
                                          }
                                        }"

                                       nil)
                              :variables {:id id}}]})
