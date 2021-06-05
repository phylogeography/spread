(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [ui.component.app-container :refer [app-container]]
            [ui.router.component :refer [page]]
            [ui.utils :refer [>evt <sub]]
            [ui.time :as time]
            [ui.subscriptions :as subs]
            [ui.router.subs :as router.subs]))

;; NOTE : just the results tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed3112972c3f62905120

;; TODO : data + error tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed305a09c542e790702f

(defn data []
  (fn []
    [:div "Data"]
    ))

(defn results []
  (fn []
    [:div "Results"]
    ))

(defmethod page :route/analysis-results []
  (let [{{:keys [id tab]} :query} (<sub [::router.subs/active-page])
        analysis                  (re-frame/subscribe [::subs/analysis-results id])]
    (fn []
      (let [{:keys [readable-name of-type created-on]} @analysis]

        (prn "@@@ analysis/results" @analysis)

        [app-container
         [:div.analysis-results
          [:div.header
           [:span readable-name]
           [:div.sub-header
            [:span (case of-type
                     "CONTINUOUS_TREE"
                     "Continuous: MCC tree"

                     "DISCRETE_TREE"
                     "Discrete: MCC tree"

                     "BAYES_FACTOR_ANALYSIS"
                     "Discrete: Bayes Factor Rates"
                     nil)]
            [:span (when created-on
                     (-> created-on time/string->date (time/format true)))]]]
          [:div.tabbed-pane
           [:div.tabs
            (map (fn [tab-name]
                   [:button.tab {:class    (when (= tab tab-name) "active")
                                 :key      tab-name
                                 :on-click #(>evt [:router/navigate :route/analysis-results nil {:id id :tab tab-name}])}
                    (case tab-name
                      "data"
                      "Data"

                      "results"
                      "Analysis results"

                      nil)])
                 ["data" "results"])]
           [:div.panel
            (case tab
              "data"    [data]
              "results" [results]
              [results])]]]]))))
