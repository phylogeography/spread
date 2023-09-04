(ns ui.new-analysis.discrete-mcc-tree
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent.core :as r]
            [shared.components :refer [button]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.input :refer [amount-input loaded-input text-input]]
            [ui.component.select :refer [attributes-select]]
            [ui.new-analysis.file-formats :as file-formats]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt debounce]]))

(defn controls [{:keys [readable-name locations-attribute most-recent-sampling-date timescale-multiplier]} {:keys [disabled?]}]
  (let [paste-disabled? (nil? @(re-frame/subscribe [::subs/pastebin]))]
    [:div.controls-wrapper
     [:div.controls {:style {:grid-area "controls"}}
      [button {:text      "Start analysis"
               :on-click  #(>evt [:discrete-mcc-tree/start-analysis {:readable-name             readable-name
                                                                     :locations-attribute-name  locations-attribute
                                                                     :most-recent-sampling-date most-recent-sampling-date
                                                                     :timescale-multiplier      timescale-multiplier}])
               :class     "golden"
               :disabled? disabled?}]
      [button {:text      "Paste settings"
               :on-click  #(>evt [:general/paste-analysis-settings])
               :class     "secondary"
               :disabled? paste-disabled?}]
      [button {:text      "Reset"
               :on-click  #(>evt [:discrete-mcc-tree/reset])
               :class     "danger"
               :disabled? disabled?}]]]))

(defn discrete-mcc-tree []
  (let [discrete-mcc-tree (re-frame/subscribe [::subs/discrete-mcc-tree])
        field-errors      (r/atom #{})]
    (fn []
      (let [{:keys [id
                    tree-file-name
                    tree-file-upload-progress
                    locations-file-name
                    locations-file-url
                    locations-file-upload-progress
                    readable-name
                    locations-attribute-name
                    most-recent-sampling-date
                    timescale-multiplier
                    attribute-names]
             :or   {timescale-multiplier 1}}
            @discrete-mcc-tree
            locations-attribute-name  (or locations-attribute-name (first attribute-names))
            most-recent-sampling-date (or most-recent-sampling-date (time/now))
            controls-disabled?        (or (not attribute-names) (not locations-file-name))]
        [:<>
         [:div.data {:style {:grid-area "data"}}
          [:section.load-tree-file
           [:div
            [:h4 "Load tree file"]
            (if (not tree-file-name)
              (if (not (pos? tree-file-upload-progress))
                [button-file-upload {:id                    "discrete-mcc-tree-file-upload-button"
                                     :label                 "Choose a file"
                                     :on-file-accepted      #(do
                                                               (swap! field-errors disj :tree-file-error)
                                                               (>evt [:discrete-mcc-tree/on-tree-file-selected %]))
                                     :file-accept-predicate file-formats/tree-file-accept-predicate
                                     :on-file-rejected      (fn [] (swap! field-errors conj :tree-file-error))}]

                [linear-progress {:value   (* 100 tree-file-upload-progress)
                                  :variant "determinate"}])

              ;; we have a file name
              [loaded-input {:value    tree-file-name
                             :on-click #(>evt [:discrete-mcc-tree/delete-tree-file])}])]
           (cond
             (contains? @field-errors :tree-file-error) [:div.field-error.button-error (str "Tree file name is over " file-formats/name-length-limit " characters long or the file content has an incorrect format.")]
             (nil? tree-file-name)                      [:p.doc "When upload is complete all unique attributes will be automatically filled. You can then select location attribute and change other settings."])]

          [:section.load-locations-file
           [:div
            [:h4 "Load locations file"]

            (if (not locations-file-name)
              (if (not (pos? locations-file-upload-progress))
                [button-file-upload {:id                    "discrete-mcc-locations-file-upload-button"
                                     :label                 "Choose a file"
                                     :disabled?             (nil? id)
                                     :on-file-accepted      #(do
                                                               (swap! field-errors disj :locations-file-error)
                                                               (>evt [:discrete-mcc-tree/on-locations-file-selected %]))
                                     :on-file-rejected      (fn [] (swap! field-errors conj :locations-file-error))
                                     :file-accept-predicate file-formats/locations-file-accept-predicate}]
                [linear-progress {:value   (* 100 locations-file-upload-progress)
                                  :variant "determinate"}])

              ;; we have a filename
              [loaded-input {:value    locations-file-name
                             :on-click #(>evt [:discrete-mcc-tree/delete-locations-file])}])]
           (cond
             (contains? @field-errors :locations-file-error) [:div.field-error.button-error (str "Locations file name is over " file-formats/name-length-limit " characters or the file content has an incorrect format.")]
             (nil? locations-file-name)                      [:p.doc "Select a file that maps geographical coordinates to the log file columns. Once this file is uploaded you can start your analysis."])]
          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names locations-file-name)
            [:div.field-table
             [:div.field-line
              [:div.field-card
               [:h4 "File info"]
               [text-input {:label     "Name" :variant :outlined
                            :value     readable-name
                            :on-change (fn [value]
                                         (debounce (>evt [:discrete-mcc-tree/set-readable-name value]) 10))}]]
              [:div.field-card
               [:h4 "Select location attributes"]
               [attributes-select {:id        "select-locations-attribute"
                                   :value     locations-attribute-name
                                   :options   attribute-names
                                   :label     "Locations attribute"
                                   :on-change (fn [value]

                                                (debounce (>evt [:discrete-mcc-tree/set-locations-attribute value]) 10))}]]]
             [:div.field-line
              [:div.field-card
               [:h4 "Most recent sampling date"]
               [date-picker {:date-format time/date-format
                             :on-change   (fn [value]
                                            (debounce (>evt [:discrete-mcc-tree/set-most-recent-sampling-date value]) 10))
                             :selected    most-recent-sampling-date}]]
              [:div.field-card
               [:h4 "Time scale"]
               [amount-input {:label       "Multiplier"
                              :value       timescale-multiplier
                              :error?      (not (nil? (:timescale-multiplier @field-errors)))
                              :helper-text (:time-scale-multiplier @field-errors)
                              :on-change   (fn [value]
                                             (debounce (>evt [:discrete-mcc-tree/set-time-scale-multiplier value]) 10))}]]]])]

         [controls {:id                        id
                    :readable-name             readable-name
                    :locations-attribute       locations-attribute-name
                    :locations-file-url        locations-file-url
                    :most-recent-sampling-date most-recent-sampling-date
                    :timescale-multiplier      timescale-multiplier}
          {:disabled? controls-disabled?}]]))))
