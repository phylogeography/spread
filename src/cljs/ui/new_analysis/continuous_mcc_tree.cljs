(ns ui.new-analysis.continuous-mcc-tree
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

(defn controls [{:keys [readable-name y-coordinate-attribute-name x-coordinate-attribute-name most-recent-sampling-date timescale-multiplier]}
                {:keys [disabled?]}]
  (let [paste-disabled? (nil? @(re-frame/subscribe [::subs/pastebin]))]
    [:div.controls-wrapper
     [:div.controls {:style {:grid-area "controls"}}
      [button {:text      "Start analysis"
               :on-click  #(>evt [:continuous-mcc-tree/start-analysis {:readable-name               readable-name
                                                                       :y-coordinate-attribute-name y-coordinate-attribute-name
                                                                       :x-coordinate-attribute-name x-coordinate-attribute-name
                                                                       :most-recent-sampling-date   most-recent-sampling-date
                                                                       :timescale-multiplier        timescale-multiplier}])
               :class     "golden"
               :disabled? disabled?}]
      [button {:text      "Paste settings"
               :on-click  #(>evt [:general/paste-analysis-settings])
               :class     "secondary"
               :disabled? paste-disabled?}]
      [button {:text      "Reset"
               :on-click  #(>evt [:continuous-mcc-tree/reset])
               :class     "danger"
               :disabled? disabled?}]]]))

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree (re-frame/subscribe [::subs/continuous-mcc-tree])
        field-errors        (r/atom #{})]
    (fn []
      (let [{:keys [id
                    readable-name
                    tree-file-name
                    tree-file-upload-progress
                    trees-file-upload-progress
                    y-coordinate-attribute-name
                    x-coordinate-attribute-name
                    most-recent-sampling-date
                    timescale-multiplier
                    attribute-names
                    time-slicer]
             :or   {timescale-multiplier 1}} @continuous-mcc-tree
            y-coordinate-attribute-name      (or y-coordinate-attribute-name (first attribute-names))
            x-coordinate-attribute-name      (or x-coordinate-attribute-name (first attribute-names))
            most-recent-sampling-date        (or most-recent-sampling-date (time/now))
            controls-disabled?               (or (not attribute-names) (not tree-file-name))
            {:keys [trees-file-name]}        time-slicer]
        [:<>
         [:div.data {}
          [:section.load-tree-file
           [:div
            [:h4 "Load tree file"]
            (if (not tree-file-name)

              (if (not (pos? tree-file-upload-progress))
                [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                     :label            "Choose a file"
                                     :on-file-accepted #(do
                                                          (swap! field-errors disj :tree-file-error)
                                                          (>evt [:continuous-mcc-tree/on-tree-file-selected %]))
                                     :on-file-rejected (fn [] (swap! field-errors conj :tree-file-error))
                                     :file-accept-predicate file-formats/tree-file-accept-predicate}]

                [linear-progress {:value   (* 100 tree-file-upload-progress)
                                  :variant "determinate"}])

              ;; we have a filename
              [loaded-input {:value    tree-file-name
                             :on-click #(>evt [:continuous-mcc-tree/delete-tree-file])}])]

           (cond
             (contains? @field-errors :tree-file-error) [:div.field-error.button-error "Tree file incorrect format."]
             (nil? tree-file-name) [:p.doc "When upload is complete all unique attributes will be automatically filled. You can then select geographical coordinates and change other settings."])]

          [:section.load-trees-file
           [:div
            [:h4 "Load trees file"]
            (if (not trees-file-name)
              (if (not (pos? trees-file-upload-progress))
                [button-file-upload {:id               "mcc-trees-file-upload-button"
                                     :label            "Choose a file"
                                     :on-file-accepted #(do
                                                          (swap! field-errors disj :trees-file-error)
                                                          (>evt [:continuous-mcc-tree/on-trees-file-selected %]))
                                     :on-file-rejected (fn [] (swap! field-errors conj :trees-file-error))
                                     :file-accept-predicate file-formats/trees-file-accept-predicate}]

                [linear-progress {:value   (* 100 trees-file-upload-progress)
                                  :variant "determinate"}])

              ;; we have a  filename
              [loaded-input {:value    trees-file-name
                             :on-click #(>evt [:continuous-mcc-tree/delete-trees-file])}])]

           (cond
             (contains? @field-errors :trees-file-error) [:div.field-error.button-error "Trees file incorrect format."]
             (nil? trees-file-name) [:p.doc "Optional: Select a file with corresponding trees distribution. This file will be used to compute a density interval around the MCC tree."])]

          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names tree-file-name)
            [:div.field-table
             [:div.field-line
              [:div.field-card
               [:h4 "File info"]
               [text-input {:label     "Name"
                            :variant :outlined
                            :value     readable-name
                            :on-change (fn [value]
                                         (>evt [:continuous-mcc-tree/set-readable-name value]))}]]]

        [:div.field-line
         [:div.field-card
          [:h4 "Select x coordinate"]
          [attributes-select {:id        "select-longitude"
                              :value     x-coordinate-attribute-name
                              :options   attribute-names
                              :label     "Longitude"
                              :on-change (fn [value]
                                           (>evt [:continuous-mcc-tree/set-x-coordinate value]))}]]
         [:div.field-card
          [:h4 "Select y coordinate"]
          [attributes-select {:id        "select-latitude"
                              :value     y-coordinate-attribute-name
                              :options   attribute-names
                              :label     "Latitude"
                              :on-change (fn [value]
                                           (>evt [:continuous-mcc-tree/set-y-coordinate value]))}]]]
        [:div.field-line
         [:div.field-card
          [:h4 "Most recent sampling date"]
          [date-picker {:date-format time/date-format
                        :on-change   (fn [value]
                                       (>evt [:continuous-mcc-tree/set-most-recent-sampling-date value]))
                        :selected    most-recent-sampling-date}]]
         [:div.field-card
          [:h4 "Time scale"]
          [amount-input {:label       "Multiplier"
                         :value       timescale-multiplier
                         :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                         :helper-text (:time-scale-multiplier @field-errors)
                         :on-change   (fn [value]
                                        (debounce (>evt [:continuous-mcc-tree/set-time-scale-multiplier value]) 10))}]]]])]
  [controls {:id                          id
             :readable-name               readable-name
             :y-coordinate-attribute-name y-coordinate-attribute-name
             :x-coordinate-attribute-name x-coordinate-attribute-name
             :most-recent-sampling-date   most-recent-sampling-date
             :timescale-multiplier        timescale-multiplier}

   {:disabled? controls-disabled? }]]))))
