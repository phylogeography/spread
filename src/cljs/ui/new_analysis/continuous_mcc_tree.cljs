(ns ui.new-analysis.continuous-mcc-tree
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [shared.components :refer [button]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.input :refer [amount-input loaded-input]]
            [ui.component.select :refer [attributes-select]]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt debounce]]))

(defn controls [{:keys [readable-name y-coordinate-attribute-name x-coordinate-attribute-name most-recent-sampling-date timescale-multiplier]}
                {:keys [disabled?]}]
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
             :on-click  #()
             :class     "secondary"
             :disabled? disabled?}]
    [button {:text      "Reset"
             :on-click  #()
             :class     "danger"
             :disabled? disabled?}]]])

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree (re-frame/subscribe [::subs/continuous-mcc-tree])
        field-errors        (re-frame/subscribe [::subs/continuous-mcc-tree-field-errors])]
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
            (cond
              (nil? tree-file-name)
              [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

              (and (not (nil? tree-file-upload-progress)) (not= 1 tree-file-upload-progress))
              [linear-progress {:value   (* 100 tree-file-upload-progress)
                                :variant "determinate"}]

              tree-file-name
              [loaded-input {:value    tree-file-name
                             :on-click #(>evt [:continuous-mcc-tree/delete-tree-file])}]

              :else nil)]
           (when (nil? tree-file-name)
             [:p.doc "When upload is complete all unique attributes will be automatically filled. You can then select geographical coordinates and change other settings."])]

          [:section.load-trees-file
           [:div
            [:h4 "Load trees file"]
            (cond
              (nil? trees-file-name)
              [button-file-upload {:id               "mcc-trees-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-trees-file-selected %])}]


              (and (not (nil? trees-file-upload-progress)) (not= 1 trees-file-upload-progress))
              [linear-progress {:value   (* 100 trees-file-upload-progress)
                                :variant "determinate"}]

              trees-file-name
              [loaded-input {:value    trees-file-name
                             :on-click #(>evt [:continuous-mcc-tree/delete-trees-file])}]

              :else nil)]

           (when (nil? trees-file-name)
             [:p.doc "Optional: Select a file with corresponding trees distribution. This file will be used to compute a density interval around the MCC tree."])]

          [:div.upload-spinner
           (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
             [circular-progress {:size 100}])]

          (when (and attribute-names tree-file-name)
            [:div.field-table
             [:div.field-line
              [:div.field-card
               [:h4 "File info"]
               [text-field {:label     "Name"
                            :value     readable-name
                            :on-change (fn [_ value]
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
