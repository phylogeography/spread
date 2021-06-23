(ns ui.new-analysis.discrete-rates
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.slider :refer [slider]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.input :refer [loaded-input]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn discrete-rates [classes]
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
             :or   {burn-in 0.1}}
            @bayes-factor]

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
           [typography {:class-name (:input-label classes)} "Load log file"]]
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
             (and (nil? log-file-upload-progress) (nil? log-file))
             [button-file-upload {:id               "bayes-factor-log-file-upload-button"
                                  :icon             :upload
                                  :class-name       (:upload-button classes)
                                  :label            "Choose a file"
                                  :on-file-accepted #(>evt [:bayes-factor/on-log-file-selected %])}]

             (not= 1 log-file-upload-progress)
             [linear-progress {:value      (* 100 log-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             log-file
             [loaded-input {:classes  classes
                            :value    log-file
                            :on-click #(>evt [:bayes-factor/delete-log-file])}]

             :else nil)]
          ;; col right
          [grid {:item true :xs 6 :xm 6}
           (when (nil? log-file)
             [:<>
              [typography "Upload log file."]
              [typography "You can then upload a matching coordinates file."]])]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true
                 :xs   6 :xm 6}
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
           (cond
             (and (nil? locations-file-upload-progress) (nil? locations-file))
             [button-file-upload {:id               "bayes-factor-locations-file-upload-button"
                                  :icon             :upload
                                  :class-name       (:upload-button classes)
                                  :label            "Choose a file"
                                  :on-file-accepted #(>evt [:bayes-factor/on-locations-file-selected %])}]

             (not= 1 locations-file-upload-progress)
             [linear-progress {:value      (* 100 locations-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             locations-file
             [loaded-input {:classes  classes
                            :value    locations-file
                            :on-click #(>evt [:bayes-factor/delete-locations-file])}]

             :else nil)]
          ;; col right
          [grid {:item true :xs 6 :xm 6}
           (when (nil? locations-file)
             [:<>
              [typography "Select a file that maps geographical coordinates to the log file columns"]
              [typography "Once this file is uploaded you can start your analysis."]])]]

         ;; row
         [grid {:container     true
                :item          true
                :xs            12 :xm 12
                :direction     :column
                :align-items   :center
                :align-content :center}
          (when (= "UPLOADING" upload-status)
            [circular-progress {:size 100}])]

         (when (and (= 1 log-file-upload-progress)
                    (= 1 locations-file-upload-progress))
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
                           :on-change (fn [_ value] (>evt [:bayes-factor/set-readable-name value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select burn-in"]]
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
              [slider {:value             burn-in
                       :min               0.0
                       :max               0.9
                       :step              0.1
                       :valueLabelDisplay :auto
                       :class-name        (:slider classes)
                       :marks             true
                       :on-change         (fn [_ value]
                                            (>evt [:bayes-factor/set-burn-in value]))}]]
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
