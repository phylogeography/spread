(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button :refer [button-with-label]]
            [ui.component.input :refer [text-input]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :refer [<sub >evt]]))

(defn data [{:keys [of-type status error] :as analysis}]
  [:div.data
   (when (= "ERROR" status)
     [:div.error
      [:span "Error occured while running analysis"]
      [:span "Check full report"]
      [:p error]])

   [:div.settings-section
    (case of-type
      "CONTINUOUS_TREE"
      (let [{:keys [readable-name x-coordinate-attribute-name y-coordinate-attribute-name
                    most-recent-sampling-date
                    timescale-multiplier]}
            analysis]
        [:div.settings
         [:fieldset
          [:legend "name"]
          [text-input {:value readable-name :read-only? true}]]

         [:div.row
          [:div.column
           [:span "y coordinate"]
           [:fieldset
            [:legend "Latitude"]
            [text-input {:value y-coordinate-attribute-name :read-only? true}]]]
          [:div.column
           [:span "x coordinate"]
           [:fieldset
            [:legend "Longitude"]
            [text-input {:value x-coordinate-attribute-name :read-only? true}]]]]

         [:div.row
          [:div.column
           [:span "Most recent sampling date"]
           [text-input {:value most-recent-sampling-date :read-only? true}]]
          [:div.column
           [:span "Time scale multiplier"]
           [text-input {:value timescale-multiplier :read-only? true}]]]])

      "DISCRETE_TREE"
      (let [{:keys [readable-name locations-attribute-name
                    most-recent-sampling-date timescale-multiplier]} analysis]
        [:div.settings
         [:fieldset
          [:legend "name"]
          [text-input {:value readable-name :read-only? true}]]

         [:div.row
          [:div.column
           [:span "locations attribute"]
           [:fieldset
            [:legend "Locations"]
            [text-input {:value locations-attribute-name :read-only? true}]]]
          [:div.column
           [:span "Most recent sampling date"]
           [text-input {:value most-recent-sampling-date :read-only? true}]]]

         [:div.row
          [:div.column
           [:span "Time scale"]
           [:fieldset
            [:legend "Multiplier"]
            [text-input {:value timescale-multiplier :read-only? true}]]]]])

      "BAYES_FACTOR_ANALYSIS"
      (let [{:keys [readable-name burn-in]} analysis]
        [:div.settings
         [:fieldset
          [:legend "name"]
          [text-input {:value readable-name :read-only? true}]]

         [:div.row
          [:div.column
           [:span "Select burn-in"]
           [:fieldset
            [:legend "Burn-in"]
            [text-input {:value burn-in :read-only? true}]]]]])

      nil)]

   [:div.buttons-section
    [button-with-label {:label    "Edit"
                        :class    :button-edit
                        :on-click #(prn "TODO : edit")}]
    [button-with-label {:label    "Copy settings"
                        :class    :button-copy-settings
                        :on-click #(prn "TODO : copy settings")}]
    [button-with-label {:label    "Delete"
                        :class    :button-delete
                        :on-click #(prn "TODO : delete")}]]])

;; NOTE : the results tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed3112972c3f62905120
(defn results [{:keys [output-file-url bayes-factors]}]
  [:div.results-tab
   [:span "Visualisations on a geographical map"]
   [:div
    [:a.button {:href output-file-url} output-file-url]
    [button-with-label {:label    "Copy"
                        :class    :button-copy-url
                        :on-click #(prn "TODO : copy to clipboard")}]]
   (when bayes-factors
     [:div
      [:div
       [:span "Support values"]
       [button-with-label {:label    "Export as CSV"
                           :class    :button-export-csv
                           :on-click #(prn "TODO : export to csv")}]]
      [:table
       [:thead [:tr
                [:th "From"]
                [:th "To"]
                [:th "Bayes Factor"]
                [:th "Posterior probability"]]]
       (doall
         (for [{:keys [from to bayes-factor posterior-probability]} bayes-factors]
           ^{:key (str from to)}
           [:tbody
            [:tr
             [:td from]
             [:td to]
             [:td bayes-factor]
             [:td posterior-probability]]]))]])])

(defmethod page :route/analysis-results []
  (let [{{:keys [id tab]} :query} (<sub [::router.subs/active-page])
        analysis                  (re-frame/subscribe [::subs/analysis-results id])]
    (fn []



      (let [{:keys [readable-name of-type created-on]} @analysis]
        [app-container

         [:div (str "RESULTS for ") id]

         #_[:div.analysis-results
          [:div.header
           [:span readable-name]
           [:div.sub-header
            [:span (case of-type
                     "CONTINUOUS_TREE"       "Continuous: MCC tree"
                     "DISCRETE_TREE"         "Discrete: MCC tree"
                     "BAYES_FACTOR_ANALYSIS" "Discrete: Bayes Factor Rates"
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
                      "data"    "Data"
                      "results" "Analysis results"
                      nil)])
                 ["data" "results"])]
           [:div.panel
            (case tab
              "data"    [data @analysis]
              "results" [results @analysis]
              [results @analysis])]]]]))))
