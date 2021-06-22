(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]

            [reagent-material-ui.core.accordion :refer [accordion]]
            [reagent-material-ui.core.accordion-details :refer [accordion-details]]
            [reagent-material-ui.core.accordion-summary :refer [accordion-summary]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.chip :refer [chip]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.icons.search :refer [search]]

            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.tab :refer [tab]]
            ;; [reagent-material-ui.core.tab-panel :refer [tab-panel]]

            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]

            [ui.component.app-container :refer [app-container]]
            [ui.component.button
             :refer
             [button-file-upload button-with-icon button-with-label]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.indicator :refer [busy]]
            [ui.component.input
             :refer
             [amount-input range-input select-input text-input]]
            [ui.component.progress :refer [progress-bar]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn error-reported [message]
  (when message
    [:div.error-reported
     [:span message]]))

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree (re-frame/subscribe [::subs/continuous-mcc-tree])
        field-errors        (re-frame/subscribe [::subs/continuous-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [id
                    readable-name
                    tree-file tree-file-upload-progress
                    trees-file trees-file-upload-progress
                    y-coordinate x-coordinate
                    most-recent-sampling-date
                    time-scale-multiplier
                    attribute-names]
             :or   {y-coordinate              (first attribute-names)
                    x-coordinate              (first attribute-names)
                    most-recent-sampling-date (time/now)
                    time-scale-multiplier     1}}
            @continuous-mcc-tree]
        [:div.continuous-mcc-tree
         [:div.upload
          [:span "Load tree file"]
          [:div
           [:div
            (cond
              (and (nil? tree-file-upload-progress) (nil? tree-file))
              [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [progress-bar {:class "tree-upload-progress-bar" :progress tree-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.tree-filename tree-file])]

           (if (nil? tree-file)
             [:p
              [:span "When upload is complete all unique attributes will be automatically filled."]
              [:span "You can then select geographical coordinates and change other settings."]]
             [button-with-icon {:on-click #(>evt [:continuous-mcc-tree/delete-tree-file])
                                :icon     :delete}])]

          [:span "Load trees file"]
          [:div
           [:div
            (cond
              (and (nil? trees-file-upload-progress) (nil? trees-file))
              [button-file-upload {:id               "mcc-trees-file-upload-button"
                                   :disabled?        (nil? attribute-names)
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-trees-file-selected %])}]

              (not= 1 trees-file-upload-progress)
              [progress-bar {:class "trees-upload-progress-bar" :progress trees-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.trees-filename trees-file])]

           (if (nil? trees-file)
             [:p
              [:span "Optional: Select a file with corresponding trees distribution."]
              [:span "This file will be used to compute a density interval around the MCC tree."]]
             [button-with-icon {:on-click #(>evt [:continuous-mcc-tree/delete-trees-file])
                                :icon     :delete}])]]

         [:div.settings
          ;; show indicator before worker parses the attributes
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [busy])

          (when attribute-names
            [:<>
             [:fieldset
              [:legend "name"]
              [text-input {:value     readable-name
                           :on-change #(>evt [:continuous-mcc-tree/set-readable-name %])}]]

             [:div.row
              [:div.column
               [:span "Select y coordinate"]
               [:fieldset
                [:legend "Latitude"]
                [select-input {:value     y-coordinate
                               :options   attribute-names
                               :on-change #(>evt [:continuous-mcc-tree/set-y-coordinate %])}]]]
              [:div.column
               [:span "Select x coordinate"]
               [:fieldset
                [:legend "Longitude"]
                [select-input {:value     x-coordinate
                               :options   attribute-names
                               :on-change #(>evt [:continuous-mcc-tree/set-x-coordinate %])}]]]]

             [:div.row
              [:div.column
               [:span "Most recent sampling date"]
               [date-picker {:date-format time/date-format
                             :on-change   #(>evt [:continuous-mcc-tree/set-most-recent-sampling-date %])
                             :selected    most-recent-sampling-date}]]

              [:div.column
               [:span "Time scale multiplier"]
               [amount-input {:class     :multiplier-field
                              :value     time-scale-multiplier
                              :on-change #(>evt [:continuous-mcc-tree/set-time-scale-multiplier %])}]
               [error-reported (:time-scale-multiplier @field-errors)]]]

             [:div.start-analysis-section
              [button-with-label {:label     "Start analysis"
                                  :class     :button-start-analysis
                                  :disabled? (seq @field-errors)
                                  :on-click  #(dispatch-n [[:continuous-mcc-tree/start-analysis {:readable-name             readable-name
                                                                                                 :y-coordinate              y-coordinate
                                                                                                 :x-coordinate              x-coordinate
                                                                                                 :most-recent-sampling-date most-recent-sampling-date
                                                                                                 :time-scale-multiplier     time-scale-multiplier}]
                                                           ;; NOTE : normally we have a running subscription already, but in case the user re-starts the analysis here we dispatch it again.
                                                           ;; it is de-duplicated by the id anyway
                                                           [:graphql/subscription {:id        id
                                                                                   :query     "subscription SubscriptionRoot($id: ID!) {
                                                                                                parserStatus(id: $id) {
                                                                                                  id
                                                                                                  status
                                                                                                  progress
                                                                                                  ofType
                                                                                                }}"
                                                                                   :variables {"id" id}}]])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(defn discrete-mcc-tree []
  (let [discrete-mcc-tree (re-frame/subscribe [::subs/discrete-mcc-tree])
        field-errors      (re-frame/subscribe [::subs/discrete-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [id
                    tree-file tree-file-upload-progress
                    locations-file locations-file-url
                    locations-file-upload-progress
                    readable-name
                    locations-attribute
                    most-recent-sampling-date
                    time-scale-multiplier
                    attribute-names]
             :or   {locations-attribute       (first attribute-names)
                    most-recent-sampling-date (time/now)
                    time-scale-multiplier     1}}
            @discrete-mcc-tree]
        [:div.discrete-mcc-tree
         [:div.upload
          [:span "Load tree file"]
          [:div
           [:div
            (cond
              (and (nil? tree-file-upload-progress) (nil? tree-file))
              [button-file-upload {:id               "discrete-mcc-tree-file-upload-button"
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:discrete-mcc-tree/on-tree-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [progress-bar {:class "tree-upload-progress-bar" :progress tree-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.tree-filename tree-file])]

           (if (nil? tree-file)
             [:p
              [:span "When upload is complete all unique attributes will be automatically filled."]
              [:span "You can then select location attribute and change other settings."]]
             [button-with-icon {:on-click #(>evt [:discrete-mcc-tree/delete-tree-file])
                                :icon     :delete}])]

          [:span "Load locations file"]
          [:div
           [:div
            (cond
              (and (nil? locations-file-upload-progress) (nil? locations-file))
              [button-file-upload {:id               "discrete-mcc-locations-file-upload-button"
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:discrete-mcc-tree/on-locations-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [progress-bar {:class "locations-upload-progress-bar" :progress locations-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.tree-filename locations-file])]

           (if (nil? locations-file)
             [:p
              [:span "Select a file that maps geographical coordinates to the location attribute states."]
              [:span "Once this file is uploaded you can start your analysis."]]
             [button-with-icon {:on-click #(>evt [:discrete-mcc-tree/delete-locations-file])
                                :icon     :delete}])]]

         [:div.settings
          ;; show indicator before worker parses the attributes
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [busy])

          (when attribute-names
            [:<>
             [:fieldset
              [:legend "name"]
              [text-input {:value     readable-name
                           :on-change #(>evt [:discrete-mcc-tree/set-readable-name %])}]]

             [:div.row
              [:div.column
               [:span "Select locations attribute"]
               [:fieldset
                [:legend "Locations"]
                [select-input {:value     locations-attribute
                               :options   attribute-names
                               :on-change #(>evt [:discrete-mcc-tree/set-locations-attribute %])}]]]
              [:div.column
               [:span "Most recent sampling date"]
               [date-picker {:date-format time/date-format
                             :on-change   #(>evt [:discrete-mcc-tree/set-most-recent-sampling-date %])
                             :selected    most-recent-sampling-date}]]]

             [:div.row
              [:div.column
               [:span "Time scale"]
               [:fieldset
                [:legend "Multiplier"]
                [amount-input {:class     :multiplier-field
                               :value     time-scale-multiplier
                               :on-change #(>evt [:discrete-mcc-tree/set-time-scale-multiplier %])}]]
               [error-reported (:time-scale-multiplier @field-errors)]]]

             [:div.start-analysis-section
              [button-with-label {:label     "Start analysis"
                                  :class     :button-start-analysis
                                  :disabled? (seq @field-errors)
                                  :on-click  #(dispatch-n [[:discrete-mcc-tree/start-analysis {:readable-name             readable-name
                                                                                               :locations-attribute-name  locations-attribute
                                                                                               :locations-file-url        locations-file-url
                                                                                               :most-recent-sampling-date most-recent-sampling-date
                                                                                               :time-scale-multiplier     time-scale-multiplier}]
                                                           [:graphql/subscription {:id        id
                                                                                   :query     "subscription SubscriptionRoot($id: ID!) {
                                                                                                parserStatus(id: $id) {
                                                                                                  id
                                                                                                  status
                                                                                                  progress
                                                                                                  ofType
                                                                                                }}"
                                                                                   :variables {"id" id}}]])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(defn discrete-rates []
  (let [bayes-factor (re-frame/subscribe [::subs/bayes-factor])
        field-errors (re-frame/subscribe [::subs/bayes-factor-field-errors])]
    (fn []
      (let [{:keys [id
                    log-file
                    log-file-upload-progress
                    locations-file
                    locations-file-url
                    locations-file-upload-progress
                    readable-name
                    burn-in
                    upload-status]
             :or   {burn-in 10}}
            @bayes-factor]
        [:div.bayes-factor
         [:div.upload
          [:span "Load log file"]
          [:div
           [:div
            (cond
              (and (nil? log-file-upload-progress) (nil? log-file))
              [button-file-upload {:id               "bayes-factor-log-file-upload-button"
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:bayes-factor/on-log-file-selected %])}]

              (not= 1 log-file-upload-progress)
              [progress-bar {:class    "log-file-upload-progress-bar"
                             :progress log-file-upload-progress
                             :label    "Uploading. Please wait"}]

              :else [:span.log-filename log-file])]

           (if (nil? log-file)
             [:p
              [:span  "Upload log file."]
              [:span  "You can then upload a matching coordinates file."]]
             [button-with-icon {:on-click #(>evt [:bayes-factor/delete-log-file])
                                :icon     :delete}])]

          [:span "Load locations file"]
          [:div
           [:div
            (cond
              (and (nil? locations-file-upload-progress) (nil? locations-file))
              [button-file-upload {:id               "bayes-factor-locations-file-upload-button"
                                   :icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:bayes-factor/on-locations-file-selected %])}]

              (not= 1 locations-file-upload-progress)
              [progress-bar {:class    "locations-upload-progress-bar"
                             :progress locations-file-upload-progress
                             :label    "Uploading. Please wait"}]

              :else [:span.locations-filename locations-file])]

           (if (nil? locations-file)
             [:p
              [:span "Select a file that maps geographical coordinates to the locations."]
              [:span "Once this file is uploaded you can then start your analysis."]]
             [button-with-icon {:on-click #(>evt [:bayes-factor/delete-locations-file])
                                :icon     :delete}])]]

         [:div.settings
          (when (= "UPLOADING" upload-status)
            [busy])

          (when (and (= 1 log-file-upload-progress)
                     (= 1 locations-file-upload-progress))
            [:<>
             [:fieldset
              [:legend "name"]
              [text-input {:value     readable-name
                           :on-change #(>evt [:bayes-factor/set-readable-name %])}]]

             [:div.row
              [:div.column
               [:span "Select burn-in"]
               [:fieldset
                [:legend "Burn-in"]
                [range-input {:value     burn-in
                              :min       0
                              :max       99
                              :on-change #(>evt [:bayes-factor/set-burn-in %])}]]]]

             [:div.start-analysis-section
              [button-with-label {:label     "Start analysis"
                                  :class     :button-start-analysis
                                  :disabled? (seq @field-errors)
                                  :on-click  #(dispatch-n [[:bayes-factor/start-analysis {:readable-name      readable-name
                                                                                          :burn-in            (/ burn-in 100)
                                                                                          :locations-file-url locations-file-url}]
                                                           [:graphql/subscription {:id        id
                                                                                   :query     "subscription SubscriptionRoot($id: ID!) {
                                                                                                parserStatus(id: $id) {
                                                                                                  id
                                                                                                  status
                                                                                                  progress
                                                                                                  ofType
                                                                                                }}"
                                                                                   :variables {"id" id}}]])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(def use-styles (styles/make-styles (fn [theme]
                                      {:centered {:display         :flex
                                                  :justify-content :center
                                                  :align-items     :center}

                                       :header {:font  "normal normal 900 24px/28px Roboto"
                                                :color "#3A3668"
                                                }

                                       :app-bar {:background    "#FFFFFF"
                                                 :box-shadow    :none
                                                 :border-bottom "1px solid #DEDEE7"
                                                 ;; :margin-bottom 30
                                                 }

                                       :tab-title {:text-transform :none
                                                   :text-align :center
                                                   :font "normal normal medium 16px/19px Roboto"
                                                   :color "#3A3668"
                                                   }

                                       :tab-subtitle {:text-transform :none
                                                      :text-align :center
                                                      :font "normal normal 900 10px/11px Roboto"
                                                      :letter-spacing "0px"
                                                      :color "#757295"
                                                      }

                                       })))

;; TODO https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/db6d1f78-c5f4-460e-9f97-32e9df007388/specs/
(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]}   @active-page
            {active-tab :tab} query
            classes           (use-styles)]
        [app-container
         [grid
          [app-bar {:position   "static"
                    :color      :transparent
                    :class-name (:app-bar classes)}
           [toolbar {:class-name (:centered classes)}
            [typography {:class-name (:header classes)} "Run new analysis"]]]
          [tabs {:value     active-tab
                 :centered  true
                 :on-change (fn [_ value]
                              (>evt [:router/navigate :route/new-analysis nil {:tab value}]))}
           [tab {:value "discrete-mcc-tree"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Discrete"]
                           [typography {:class-name (:tab-subtitle classes)} "MCC tree"]])}]
           [tab {:value "discrete-rates"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Discrete"]
                           [typography {:class-name (:tab-subtitle classes)} "Rates"]])}]
           [tab {:value "continuous-mcc-tree"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Continuous"]
                           [typography {:class-name (:tab-subtitle classes)} "MCC tree"]])}]]
          (case active-tab
            "discrete-mcc-tree"   [discrete-mcc-tree]
            "discrete-rates"      [discrete-rates]
            "continuous-mcc-tree" [continuous-mcc-tree]
            [continuous-mcc-tree])]]))))
