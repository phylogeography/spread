(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [shared.components :refer [button collapsible-tab tabs]]
            [ui.analysis-results.continuous-mcc-tree :refer [continuous-mcc-tree]]
            [ui.analysis-results.discrete-mcc-tree :refer [discrete-mcc-tree]]
            [ui.analysis-results.discrete-rates :refer [discrete-rates]]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button :refer [button-file-upload]]
            [ui.new-analysis.file-formats :as file-formats]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :refer [<sub >evt]]))

(def type->label {"CONTINUOUS_TREE"       ["Continuous:" "MCC tree"]
                  "DISCRETE_TREE"         ["Discrete:" "MCC tree"]
                  "BAYES_FACTOR_ANALYSIS" ["Discrete:" "Bayes factor rates"]})

(defn data [{:keys [of-type error] :as analysis}]
  [:div.data
   (when error
         [:div.error
          [:img {:src "icons/icn_error.svg"}]
          [:div
           [:span.title "Error ocurred while running analysis"]
           [collapsible-tab (cond-> {:id :analysis-result-error-check-report
                                     :title "Check full report"
                                     :child [:div.check-full-report
                                             error]})]]])

   (case of-type
     "CONTINUOUS_TREE"
     [continuous-mcc-tree analysis]
     "DISCRETE_TREE"
     [discrete-mcc-tree analysis]
     "BAYES_FACTOR_ANALYSIS"
     [discrete-rates analysis]
     nil)])

(defn get-custom-map-file-id [url]
  (when url
    (let [[_ file-id] (re-find #".+/(.+.json)" url)]
      file-id)))

(defn results [{:keys [bayes-factors custom-map] :as analysis}]
  (let [config @(re-frame/subscribe [::subs/config])
        user @(re-frame/subscribe [::subs/authorized-user])
        viewer-host (:analysis-viewer-url config)
        {:keys [viewer-url-params]} (:analysis analysis)
        custom-map-path (when custom-map
                          (let [custom-map-file-id (get-custom-map-file-id (:file-url custom-map))]
                            (str (:id user) "/" custom-map-file-id)))
        viewer-url (cond-> (str viewer-host "/" viewer-url-params)
                     custom-map-path (str "&custom_map=" custom-map-path))]
    [:div.results
     [:section.visualization
      [:h4 "Visualisations on a geographical map"]
      [:div
       [:span.link viewer-url]
       [button {:text "Copy"
                :on-click #(js/navigator.clipboard.writeText viewer-url)
                :class "golden"}]]]

     [:section.custom-map
      [:h4 "Custom map"]
      (if custom-map
        [:div.map
         [:div.map-data
          [:div
           [:span.label "File name:"] [:span (:file-name custom-map)]]
          [:div
           [:span.label "Url:"] [:span.link (:file-url custom-map)]]]
         [button {:text "Remove"
                  :on-click #(>evt [:analysis-results/delete-custom-map (:id analysis)])
                  :class "danger"}]]
        [button-file-upload {:id               "custom-map-upload-button"
                             :label            "Upload custom map"
                             :on-file-accepted (fn [file-meta]
                                                 (js/console.log (str (assoc file-meta
                                                                             :analysis-id (:id analysis))))
                                                 (>evt [:analysis-results/on-custom-map-file-selected (assoc file-meta
                                                                                                             :analysis-id (:id analysis))]))
                             :on-file-rejected (fn [])
                             :file-accept-predicate file-formats/custom-map-file-accept-predicate}])]

     [:section.table
      (when bayes-factors
        [:div
         [:div.export
          [:span "Support values"]
          [button {:text "Export To CSV"
                   :on-click #(dispatch [:analysis-results/export-bayes-table-to-csv bayes-factors])
                   :class "secondary"}]]
         ;; TODO : table with sorting https://material-ui.com/components/tables/#sorting-amp-selecting
         [table-container {}
          [table {}
           [table-head
            [table-row
             [table-cell {:align :right} "From"]
             [table-cell {:align :right} "To"]
             [table-cell {:align :right} "Bayes Factor"]
             [table-cell {:align :right} "Posterior probability"]]]
           [table-body
            (doall
              (map (fn [{:keys [from to bayes-factor posterior-probability]}]
                     [table-row {:key (str from to)}
                      [table-cell {:align :right} from]
                      [table-cell {:align :right} to]
                      [table-cell {:align :right} bayes-factor]
                      [table-cell {:align :right} posterior-probability]])
                   bayes-factors))]]]])]]))

(defn tab-pane [{:keys [id]}]
  (let [analysis (re-frame/subscribe [::subs/analysis-results id])]
    (fn [{:keys [id active-tab classes]}]
      [:div
       [:div.tabs-wrapper {:style {:display :flex
                                   :align-items :center}}
        [tabs {:on-change (fn [_ value]
                                (>evt [:router/navigate :route/analysis-results nil {:id id :tab value}]))
                   :active active-tab
                   :tabs-vec [{:id "data"    :label "Data" }
                              {:id "results" :label "Analysis results"}]}]]
       (case active-tab
         "data"    [data @analysis]
         "results" [results @analysis]
         [results @analysis classes])])))

(defn analysis-header [{:keys [readable-name of-type created-on]}]
  (let [[label text] (type->label of-type)
        date (when created-on (time/format-date-str created-on))
        time (when created-on (time/format-time-str created-on))]
    [:div.analysis-header {:style {:grid-area "header"}}
     [:div.readable-name {:style {:grid-area "readable-name"}} readable-name]
     [:div.sub-name {:style {:grid-area "sub-name"}}
      [:span.label label] [:span.text text]]
     [:div.datetime {:style {:grid-area "datetime"}}
      [:span.date date] [:span.time time]]]))

(defn analysis-body [active-page]
  (let [{{:keys [id] :as query} :query} active-page
        active-tab (or (:tab query) "results")]
    [:div.body {:style {:grid-area "body"}}
     [tab-pane {:id         id
                :active-tab active-tab}]]))

(defn footer [{{:keys [id]} :query}]
  [:div.footer-wrapper
   [:div.footer {:style {:grid-area "footer"}}
    [button {:text "Copy settings"
             :on-click #(>evt [:general/copy-analysis-settings id])
             :class "secondary"}]
    [button {:text "Delete"
             :on-click #(>evt [:graphql/query {:query
                                               "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                                                   deleteAnalysis(id: $analysisId) {
                                                                     id
                                                                   }
                                                                 }"
                                               :variables {:analysisId id}}])
             :class "danger"}]]])

(defmethod page :route/analysis-results []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{{:keys [id]} :query}                         @active-page
            {:keys [readable-name] :as analysis} (<sub [::subs/analysis-results id])]
        (if-not readable-name
          [:div "Loading..." ]
          [app-container
           [:div.analysis-results
            [analysis-header analysis]
            [analysis-body @active-page]
            [footer @active-page]]])))))
