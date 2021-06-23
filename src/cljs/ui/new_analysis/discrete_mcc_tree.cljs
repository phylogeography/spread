(ns ui.new-analysis.discrete-mcc-tree
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.input :refer [amount-input loaded-input]]
            [ui.component.select :refer [attributes-select]]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn discrete-mcc-tree [classes]
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

        ;; main container
        [grid {:container true
               :direction :column
               :spacing   1}

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true
                 :xs   6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load tree file"]]
          ;; col right
          [grid {:item true :xs 6 :xm 6}]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true :xs 6 :xm 6}
           (cond
             (and (nil? tree-file-upload-progress) (nil? tree-file))
             [button-file-upload {:id               "discrete-mcc-tree-file-upload-button"
                                  :label            "Choose a file"
                                  :class-name       (:upload-button classes)
                                  :icon             :upload
                                  :on-file-accepted #(>evt [:discrete-mcc-tree/on-tree-file-selected %])}]

             (not= 1 tree-file-upload-progress)
             [linear-progress {:value      (* 100 tree-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             tree-file
             [loaded-input {:classes  classes
                            :value    tree-file
                            :on-click #(>evt [:discrete-mcc-tree/delete-tree-file])}]

             :else nil)]
          ;; col right
          [grid {:item true
                 :xs   6 :xm 6}
           (when (nil? tree-file)
             [:<>
              [typography "When upload is complete all unique attributes will be automatically filled."]
              [typography "You can then select location attribute and change other settings."]])]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}

          ;; col left
          [grid {:item true :xs 6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load locations file"]]

          ;; col right
          [grid {:item true :xs 6 :xm 6}]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true :xs 6 :xm 6}
           [button-file-upload {:id               "discrete-mcc-locations-file-upload-button"
                                :icon             :upload
                                :class-name       (:upload-button classes)
                                :label            "Choose a file"
                                :on-file-accepted #(>evt [:discrete-mcc-tree/on-locations-file-selected %])}]]

          ;; col right
          [grid {:item true
                 :xs   6 :xm 6}
           (when (nil? locations-file)
             [:<>
              [typography "Select a file that maps geographical coordinates to the location attribute states"]
              [typography "Once this file is uploaded you can start your analysis."]])]]

         ;; row
         [grid {:container     true
                :item          true
                :xs            12 :xm 12
                :direction     :column
                :align-items   :center
                :align-content :center}
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [circular-progress {:size 100}])]

         (when (and attribute-names locations-file)
           [:<>
            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [text-field {:label     "Name" :variant :outlined
                           :value     readable-name
                           ;; :shrink    (nil? readable-name)
                           :on-change (fn [_ value] (>evt [:discrete-mcc-tree/set-readable-name value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select locations attribute"]]
             ;; col right
             [grid {:item true
                    :xs   6 :xm 6}
              [typography {:class-name (:input-label classes)} "Most recent sampling date"]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}

             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [attributes-select {:classes   classes
                                  :id        "select-locations-attribute"
                                  :value     locations-attribute
                                  :options   attribute-names
                                  :label     "Locations attribute"
                                  :on-change (fn [value]
                                               (>evt [:discrete-mcc-tree/set-locations-attribute value]))}]]
             ;; col right
             [grid {:item true
                    :xs   6 :xm 6}
              [date-picker {:wrapperClassName (:date-picker classes)
                            :date-format      time/date-format
                            :on-change        #(>evt [:discrete-mcc-tree/set-most-recent-sampling-date %])
                            :selected         most-recent-sampling-date}]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [typography {:class-name (:input-label classes)} "Time scale"]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [amount-input {:label       "Multiplier"
                             :value       time-scale-multiplier
                             :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                             :helper-text (:time-scale-multiplier @field-errors)
                             :on-change   (fn [value]
                                            (>evt [:discrete-mcc-tree/set-time-scale-multiplier value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            [box {:padding-top    5
                  :padding-bottom 5}
             [divider {:variant "fullWidth"}]]

            [grid {:container true
                   :direction :row
                   :spacing   1}
             [grid {:item true}
              [button {:variant   :contained
                       :disabled  (boolean (seq @field-errors))
                       :color     :primary
                       :size      :large
                       :className (:start-button classes)
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
                                                                        :variables {"id" id}}]])}
               "Start analysis"]]

             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :className (:start-button classes)
                       :on-click  #(prn "TODO")}
               "Paste settings"]]

             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :className (:start-button classes)
                       :on-click  #(prn "TODO")}
               "Reset"]]]])]))))
