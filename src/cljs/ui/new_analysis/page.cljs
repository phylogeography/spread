(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button
             :refer
             [button-file-upload button-with-icon button-with-label]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.indicator :refer [busy]]
            [ui.component.input :refer [amount-input select-input text-input range-input]]
            [ui.component.progress :refer [progress-bar]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt]]))

(defn error-reported [message]
  (when message
    [:div.error-reported
     [:span message]]))

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree    (re-frame/subscribe [::subs/continuous-mcc-tree])
        continuous-tree-parser (re-frame/subscribe [::subs/active-continuous-tree-parser])
        field-errors           (re-frame/subscribe [::subs/continuous-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [attribute-names]} @continuous-tree-parser
            {:keys [tree-file tree-file-upload-progress readable-name
                    y-coordinate x-coordinate
                    most-recent-sampling-date
                    time-scale-multiplier]
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
                                  :on-click  #(>evt [:continuous-mcc-tree/start-analysis {:readable-name             readable-name
                                                                                          :y-coordinate              y-coordinate
                                                                                          :x-coordinate              x-coordinate
                                                                                          :most-recent-sampling-date most-recent-sampling-date
                                                                                          :time-scale-multiplier     time-scale-multiplier}])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(defn discrete-mcc-tree []
  (let [discrete-mcc-tree    (re-frame/subscribe [::subs/discrete-mcc-tree])
        discrete-tree-parser (re-frame/subscribe [::subs/active-discrete-tree-parser])
        field-errors         (re-frame/subscribe [::subs/discrete-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [attribute-names]} @discrete-tree-parser
            {:keys [tree-file tree-file-upload-progress
                    locations-file locations-file-url locations-file-upload-progress
                    readable-name
                    locations-attribute
                    most-recent-sampling-date
                    time-scale-multiplier]
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
                                  :on-click  #(>evt [:discrete-mcc-tree/start-analysis {:readable-name             readable-name
                                                                                        :locations-attribute-name  locations-attribute
                                                                                        :locations-file-url        locations-file-url
                                                                                        :most-recent-sampling-date most-recent-sampling-date
                                                                                        :time-scale-multiplier     time-scale-multiplier}])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(defn discrete-rates []
  (let [bayes-factor        (re-frame/subscribe [::subs/bayes-factor])
        bayes-factor-parser (re-frame/subscribe [::subs/active-bayes-factor-parser])
        field-errors        (re-frame/subscribe [::subs/bayes-factor-field-errors])]
    (fn []
      (let [{:keys [status]} @bayes-factor-parser
            {:keys [log-file
                    log-file-upload-progress
                    locations-file
                    locations-file-url
                    locations-file-upload-progress
                    readable-name
                    burn-in]
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
              [progress-bar {:class "log-file-upload-progress-bar" :progress log-file-upload-progress :label "Uploading. Please wait"}]

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
              [progress-bar {:class "locations-upload-progress-bar" :progress locations-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.locations-filename locations-file])]

           (if (nil? locations-file)
             [:p
              [:span "Select a file that maps geographical coordinates to the locations."]
              [:span "Once this file is uploaded you can then start your analysis."]]
             [button-with-icon {:on-click #(>evt [:bayes-factor/delete-locations-file])
                                :icon     :delete}])]]

         [:div.settings
          (when (= "UPLOADING" status)
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
                                  :on-click  #(>evt [:bayes-factor/start-analysis {:readable-name      readable-name
                                                                                   :burn-in            (/ burn-in 100)
                                                                                   :locations-file-url locations-file-url}])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))


(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]}   @active-page
            {active-tab :tab} query]
        [app-container
         [:div.new-analysis
          [:span "Run new analysis"]
          [:div.tabbed-pane
           [:div.tabs
            (map (fn [tab]
                   [:button.tab {:class    (when (= active-tab tab) "active")
                                 :key      tab
                                 :on-click #(>evt [:router/navigate :route/new-analysis nil {:tab tab}])}
                    (case tab
                      "discrete-mcc-tree"
                      [:div
                       [:span "Discrete"]
                       [:span "MCC tree"]]

                      "discrete-rates"
                      [:div
                       [:span "Discrete"]
                       [:span "Rates"]]

                      "continuous-mcc-tree"
                      [:div
                       [:span "Continuous"]
                       [:span "MCC tree"]]

                      ;; "continuous-time-slices"
                      ;; [:div
                      ;;  [:span "Continuous"]
                      ;;  [:span "Time slices"]]
                      nil)])
                 ["discrete-mcc-tree" "discrete-rates" "continuous-mcc-tree" "continuous-time-slices"])]
           [:div.panel
            (case active-tab
              "discrete-mcc-tree"   [discrete-mcc-tree]
              "discrete-rates"      [discrete-rates]
              "continuous-mcc-tree" [continuous-mcc-tree]
              ;; "continuous-time-slices" [continuous-time-slices]
              [continuous-mcc-tree])]]]]))))
