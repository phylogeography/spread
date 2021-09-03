(ns ui.events.analysis-results)

(defn initialize-page
  [_]
  ;; NOTE : dispatch query when all results loaded
  ;; loading is triggered by genreal/initialize which dispatches a graphql query
  {:async-flow {:first-dispatch [:analysis-results/initial-query]
                ;; NOTE if no data in app db try again after it's loaded
                :rules          [{:when     :seen?
                                  :events   [:user-analysis-loaded]
                                  :dispatch [:analysis-results/initial-query]}]}})

(defn initial-query
  [{:keys [db]}]
  (let [id                (-> db :ui.router :active-page :query :id)
        {:keys [of-type]} (get-in db [:analysis id])]
    (when of-type
      {:dispatch [:graphql/query {:query (case (keyword of-type)
                                           :CONTINUOUS_TREE
                                           "query GetContinuousTree($id: ID!) {
                                                        getContinuousTree(id: $id) {
                                                          id
                                                          readableName
                                                          attributeNames
                                                          outputFileUrl
                                                          xCoordinateAttributeName
                                                          yCoordinateAttributeName
                                                          mostRecentSamplingDate
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
                                                          burnIn
                                                          bayesFactors {
                                                            from
                                                            to
                                                            bayesFactor
                                                            posteriorProbability}
                                                          outputFileUrl
                                                          analysis {
                                                              viewerUrlParams
                                                          }
                                                        }
                                                      }"

                                           nil)
                                  :variables {:id id}}]})))
