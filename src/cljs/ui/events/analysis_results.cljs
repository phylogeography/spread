(ns ui.events.analysis-results
  (:require [clojure.string :as str]
   [ui.file-fxs]))

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

(defn export-bayes-table-to-csv [_ [_ bayes-factors]]
  (let [first-row (first bayes-factors)
        headers (keys first-row)
        headers-row (str/join "," (map name headers))
        data (->> bayes-factors
                  (map (fn [row]
                         (->> headers
                              (map #(get row %))
                              (str/join ","))))
                  (str/join "\n"))
        file-content (str headers-row "\n" data )]
    {:spread/download-text-as-file {:text file-content
                                    :file-name "bayes-data.csv"
                                    :file-type "data:text/csv;charset=utf-8"}}))
