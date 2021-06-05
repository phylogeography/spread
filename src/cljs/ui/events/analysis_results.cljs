(ns ui.events.analysis-results)

(defn initialize-page
  [{:keys [db]}]
  (let [id                (-> db :ui.router :active-page :query :id)
        {:keys [of-type] :as analysis} (get-in db [:analysis id])]

    (prn id analysis)

    ;; TODO : getAnalyis(id) -> Analysis

    (when id
      {:dispatch [:graphql/query {:query (case (keyword of-type)

                                           ;; TODO : define all neccesary fields
                                           :CONTINUOUS_TREE
                                           "query GetContinuousTree($id: ID!) {
                                                        getContinuousTree(id: $id) {
                                                          id
                                                          attributeNames
                                                        }
                                                      }"

                                           :DISCRETE_TREE
                                           "query GetDiscreteTree($id: ID!) {
                                                        getDiscreteTree(id: $id) {
                                                          id
                                                          attributeNames
                                                        }
                                                      }"

                                           :BAYES_FACTOR_ANALYSIS
                                           "query GetBayesFactorAnalysis($id: ID!) {
                                                        getBayesFactorAnalysis(id: $id) {
                                                          id
                                                        }
                                                      }"

                                           nil)
                                  :variables {:id id}}]})))
