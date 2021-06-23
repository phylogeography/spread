(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.outlined-input :refer [outlined-input]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.form-control :refer [form-control]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.input-adornment :refer [input-adornment]]
            [reagent-material-ui.core.input-label :refer [input-label]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.outlined-input :refer [outlined-input]]
            [reagent-material-ui.core.select :refer [select]]
            [reagent-material-ui.core.slider :refer [slider]]
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]

            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-pagination :refer [table-pagination]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.table-sort-label :refer [table-sort-label]]

            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
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




(def use-styles (styles/make-styles (fn [theme]
                                      {

                                       :centered {:display         :flex
                                                  :justify-content :center
                                                  :align-items     :center}

                                       :header {:font  "normal normal 900 24px/28px Roboto"
                                                :color "#3A3668"}

                                       :app-bar {:background    "#FFFFFF"
                                                 :box-shadow    :none
                                                 :border-bottom "1px solid #DEDEE7"}

                                       :box {
                                             ;; :padding-right  5
                                             ;; :text-align     "left"
                                             ;; :font           "normal normal medium 16px/19px Roboto"
                                             ;; :font-weight    500
                                             ;; :letter-spacing "0px"
                                             ;; :color          "#3A3668"

                                             :display        :flex
                                             :flex-direction :row
                                             ;; :align-items :center
                                             ;; :justify-content :space-between
                                             }

                                       :input-label {:font        "normal normal medium 16px/19px Roboto"
                                                     :color       "#3A3668"
                                                     :font-weight :bold}

                                       :copy-button {:height 38
                                                     :text-transform :none
                                                     :border-radius "8px"}

                                       :analysis-viewer-url {:height 38
                                                             :min-width 500
                                                             :border-radius "8px"
                                                             :font   "normal normal medium 14px/16px Roboto"
                                                             :color  "#3A3668"}

                                       :divider {:width "100%"}

                                       :scroll-list {:overflow   :auto
                                                     :max-height 300}

                                       })))


;; NOTE : the results tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed3112972c3f62905120
(defn results [{:keys [output-file-url bayes-factors]} classes]
  [grid {:container true
         :direction :column}
   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [typography {:class-name (:input-label classes)} "Visualisations on a geographical map"]]
    [grid {:item true
           :xs   6 :xm 6}]]
   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [outlined-input {:class-name (:analysis-viewer-url classes)
                      :variant    :outlined
                      ;; TODO : link to viewer here
                      :value      (or output-file-url "")}]]
    [grid {:item true
           :xs   6 :xm 6}
     [button {:variant   "contained"
              :color     "primary"
              :size      "large"
              :className (:copy-button classes)
              :on-click  #(prn "TODO")}
      "Copy"]]]

   ;; bayes factors table
   (when bayes-factors
     [:<>
      [box {:paddingTop    10
            :paddingBottom 10}
       [divider {:variant    "fullWidth"
                 :class-name (:divider classes)}]]
      [grid {:container true
             :item      true
             :direction :row
             :xs        12 :xm 12}
       [grid {:item true
              :xs   6 :xm 6}
        [typography {:class-name (:input-label classes)} "Support values"]]
       [grid {:item true
              :xs   6 :xm 6}
        [button {:variant   "contained"
                 :color     "primary"
                 :size      "large"
                 :className (:export-button classes)
                 :on-click  #(prn "TODO")}
         "Export to CSV"]]]

      [grid {:container true
             :item      true
             :direction :row
             :xs        12 :xm 12}
       ;; TODO : table with sorting https://material-ui.com/components/tables/#sorting-amp-selecting
       [table-container {:class-name (:scroll-list classes)}
        [table {:class-name (:table classes)}
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
                 bayes-factors))]]]]])

   ;; buttons
   [box {:paddingTop    10
         :paddingBottom 10}
    [divider {:variant    "fullWidth"
              :class-name (:divider classes)}]]
   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Edit"]]
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Copy settings"]]
    [grid {:item true
           :xs   4 :xm 4}
     [button {:variant   :contained
              :color     "primary"
              :size      :large
              :className (:start-button classes)
              :on-click  #(prn "TODO")}
      "Delete"]]]





   ])

(defn tab-pane [{:keys [id active-tab classes]}]
  (let [analysis (re-frame/subscribe [::subs/analysis-results id])]
    (fn [{:keys [id active-tab classes]}]
      [:<>
       [tabs {:value     active-tab
              :centered  true
              :classes   {:indicator (:indicator classes)}
              :on-change (fn [_ value]
                           (>evt [:router/navigate :route/analysis-results nil {:id id :tab value}]))}
        [tab {:value "data"
              :label "Data"}]
        [tab {:value "results"
              :label "Analysis results"}]]
       (case active-tab
         "data"    [data @analysis classes]
         "results" [results @analysis classes]
         [results @analysis classes])])))

(defmethod page :route/analysis-results []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{{:keys [id] :as query} :query}                         @active-page
            {:keys [readable-name of-type created-on] :as analysis} (<sub [::subs/analysis-results id])
            active-tab                                              (or (:tab query) "results")
            classes                                                 (use-styles)]
        (if-not readable-name
          [:div "Loading..." ]
          [app-container
           [grid
            [app-bar {:position   "static"
                      :color      :transparent
                      :class-name (:app-bar classes)}
             [toolbar {:class-name (:centered classes)}
              ;; main
              [grid {:container     true
                     :direction     :column
                     :align-items   :center
                     :align-content :center}
               ;; gutter
               [grid {:item true :xs 2 :xm 2}]
               ;; row
               [grid {:item true :xs 8 :xm 8}
                [typography {:class-name (:header classes)} readable-name]
                ;; row
                [grid {:item      true
                       :container true
                       :spacing   10}
                 [grid {:item true}
                  (case of-type
                    "CONTINUOUS_TREE"       [box {:class-name (:box classes)}
                                             [typography "Continuous:"]
                                             [typography "MCC tree"]]
                    "DISCRETE_TREE"         [box {:class-name (:box classes)}
                                             [typography "Discrete:"]
                                             [typography "MCC tree"]]
                    "BAYES_FACTOR_ANALYSIS" [box {:class-name (:box classes)}
                                             [typography "Discrete:"]
                                             [typography "Bayes factor rates"]]
                    nil)]
                 [grid {:item true}
                  [typography  (when created-on
                                 (-> created-on time/string->date (time/format true)))]]]]
               ;; gutter
               [grid {:item true :xs 2 :xm 2}]]]]
            [tab-pane {:id         id
                       ;; :analysis   analysis
                       :active-tab active-tab
                       :classes    classes}]]])))))


#_[:div.analysis-results

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
       [results @analysis])]]]
