(ns ui.events.analysis-results)

(defn initialize-page
  [{:keys [db]}]
  ;; NOTE : dispatch query when all results loaded
  ;; loading is triggered by genreal/initialize which dispatches a graphql query
  {:async-flow {:first-dispatch [:do-nothing]
                :rules          [{:when     :seen?
                                  :events   [:user-analysis-loaded]
                                  :dispatch [:analysis-results/initial-query]}]}})

(defn initial-query
  "if user opens home page we subscribe to all ongoing analysis"
  [{:keys [db]}]
  (let [id                             (-> db :ui.router :active-page :query :id)
        {:keys [of-type] :as analysis} (get-in db [:analysis id])]

    (prn analysis)

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
                                :variables {:id id}}]}))
