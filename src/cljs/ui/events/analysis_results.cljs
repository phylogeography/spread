(ns ui.events.analysis-results)

(defn initialize-page
  [_]
  ;; NOTE : dispatch query when all results loaded
  ;; loading is triggered by general/initialize which dispatches a graphql query
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
      {:dispatch [:general/query-analysis {:id id :of-type of-type}]})))
