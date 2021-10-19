(ns ui.events.new-analysis)

(defn initialize-page
  [_]
  ;; NOTE : dispatch query when all results loaded
  ;; loading is triggered by general/initialize which dispatches a graphql query
  {:async-flow {:first-dispatch [:new-analysis/initial-query]
                ;; NOTE if no data in app db try again after it's loaded
                :rules          [{:when     :seen?
                                  :events   [:user-analysis-loaded]
                                  :dispatch [:new-analysis/initial-query]}]}})

(defn initial-query
  [{:keys [db]}]
  (let [id                (-> db :ui.router :active-page :query :id)
        {:keys [of-type]} (get-in db [:analysis id])]
    (when of-type
      {:db       (assoc-in db [:new-analysis (case of-type
                                               "DISCRETE_TREE"         :discrete-mcc-tree
                                               "CONTINUOUS_TREE"       :continuous-mcc-tree
                                               "BAYES_FACTOR_ANALYSIS" :bayes-factor)
                         :id] id)
       :dispatch [:general/query-analysis {:id id :of-type of-type}]})))
