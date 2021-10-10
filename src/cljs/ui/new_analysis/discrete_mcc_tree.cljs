(ns ui.new-analysis.discrete-mcc-tree
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [shared.components :refer [button]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.input :refer [amount-input loaded-input text-input]]
            [ui.component.select :refer [attributes-select]]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dispatch-n debounce]]))

(defn controls [{:keys [id readable-name locations-attribute locations-file-url most-recent-sampling-date time-scale-multiplier]} {:keys [disabled?]}]
  [:div.controls-wrapper
   [:div.controls {:style {:grid-area "controls"}}
    [button {:text "Start analysis"
             :on-click #(dispatch-n [[:discrete-mcc-tree/start-analysis {:readable-name             readable-name
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
                                                                           }
                                                                         }"
                                                             :variables {"id" id}}]])
             :class "golden"
             :disabled? disabled?}]
    [button {:text "Paste settings"
             :on-click #()
             :class "secondary"
             :disabled? disabled?}]
    [button {:text "Reset"
             :on-click #(js/alert "Yeahhhhhhh")
             :class "danger"
             :disabled? disabled?}]]])

(defn discrete-mcc-tree []
  (let [discrete-mcc-tree (re-frame/subscribe [::subs/discrete-mcc-tree])
        field-errors      (re-frame/subscribe [::subs/discrete-mcc-tree-field-errors])]
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
                    time-scale-multiplier
                    attribute-names]} @discrete-mcc-tree
            locations-attribute-name  (or locations-attribute-name (first attribute-names))
            most-recent-sampling-date (or

                                        ;; TODO : parse again
                                        most-recent-sampling-date

                                        (time/now))
            time-scale-multiplier     (or time-scale-multiplier     1)
            controls-disabled?        (or (not attribute-names) (not locations-file-name))
            ]

        ;; (prn "@1" @discrete-mcc-tree)
        (prn "@render 1" most-recent-sampling-date)

        (prn "@render 2" most-recent-sampling-date)

        [:<>
         [:div.data {:style {:grid-area "data"}}
          [:section.load-tree-file
           [:div
            [:h4 "Load tree file"]
            (cond
              (and #_(nil? tree-file-upload-progress) (nil? tree-file-name))
              [button-file-upload {:id               "discrete-mcc-tree-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:discrete-mcc-tree/on-tree-file-selected %])}]

              (and (not (nil? tree-file-upload-progress)) (not= 1 tree-file-upload-progress))
              [linear-progress {:value      (* 100 tree-file-upload-progress)
                                :variant    "determinate"}]

              tree-file-name
              [loaded-input {:value    tree-file-name
                             :on-click #(>evt [:discrete-mcc-tree/delete-tree-file])}]

              :else nil)]
           (when (nil? tree-file-name)
             [:p.doc "When upload is complete all unique attributes will be automatically filled. You can then select location attribute and change other settings."])]
          [:section.load-locations-file
           [:div
            [:h4 "Load locations file"]
            (cond
              (nil? locations-file-name)
              [button-file-upload {:id               "discrete-mcc-locations-file-upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:discrete-mcc-tree/on-locations-file-selected %])}]

              (and (not (nil? locations-file-upload-progress)) (not= 1 locations-file-upload-progress))
              [linear-progress {:value   (* 100 locations-file-upload-progress)
                                :variant "determinate"}]

              locations-file-name
              [loaded-input {:value    locations-file-name
                             :on-click #(>evt [:discrete-mcc-tree/delete-locations-file])}]

              :else nil)]]
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
               [date-picker {:date-format      time/date-format
                             :on-change        (fn [value]
                                                 (debounce (>evt [:discrete-mcc-tree/set-most-recent-sampling-date value]) 10))
                             :selected         most-recent-sampling-date}]]
              [:div.field-card
               [:h4 "Time scale"]
               [amount-input {:label       "Multiplier"
                              :value       time-scale-multiplier
                              :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                              :helper-text (:time-scale-multiplier @field-errors)
                              :on-change   (fn [value]
                                             (debounce (>evt [:discrete-mcc-tree/set-time-scale-multiplier value]) 10))}]]]])]

         [controls {:id id
                    :readable-name readable-name
                    :locations-attribute locations-attribute-name
                    :locations-file-url locations-file-url
                    :most-recent-sampling-date most-recent-sampling-date
                    :time-scale-multiplier time-scale-multiplier}
          {:disabled? controls-disabled?}]]))))
