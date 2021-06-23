(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
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
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.component.input :refer [amount-input]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

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
                                                 }

                                       :tab-title {:text-transform :none
                                                   :text-align     :center
                                                   :font           "normal normal medium 16px/19px Roboto"
                                                   :color          "#3A3668"
                                                   }

                                       :tab-subtitle {:text-transform :none
                                                      :text-align     :center
                                                      :font           "normal normal 900 10px/11px Roboto"
                                                      :letter-spacing "0px"
                                                      :color          "#757295"
                                                      }

                                       :indicator {:background       "#EEBE53"
                                                   :background-color "#EEBE53"}

                                       :upload-button {:textTransform  "none"
                                                       :font           "normal normal medium 16px/19px Roboto"
                                                       :letter-spacing "0px"
                                                       :background     "#3428CA"
                                                       :color          "#ECEFF8"
                                                       :border-radius  "8px"
                                                       :width          "324px"
                                                       :height         "48px"
                                                       }

                                       :upload-progress {:border-radius "8px"
                                                         :width         "324px"
                                                         :height        "48px"
                                                         }

                                       :border {
                                                :border        "1px solid #DEDEE7"
                                                ;; :width "100%"
                                                :display       :flex
                                                ;; :justify-content :center
                                                :align-items   :center
                                                :width         "373px"
                                                :height        "46px"
                                                :border-radius "8px"
                                                }

                                       :icon-button {:width  "14px"
                                                     :height "18px"
                                                     }

                                       :input-label {
                                                     :font        "normal normal medium 16px/19px Roboto"
                                                     :color       "#3A3668"
                                                     :font-weight :bold
                                                     }

                                       :outlined-input {:height "46px"
                                                        :font   "normal normal medium 14px/16px Roboto"
                                                        :color  "#3A3668"
                                                        }

                                       :form-control {:margin    ((:spacing theme) 1)
                                                      :min-width 120
                                                      }

                                       :date-picker {:border-radius "8px"
                                                     :border        "1px solid #E2E2EA"
                                                     }

                                       :start-button {:background     "EEBE53"
                                                      :box-shadow     "0px 10px 30px #EEBE5327"
                                                      :border-radius  "8px"
                                                      :font           "normal normal medium 16px/19px Roboto"
                                                      :color          "#3A3668"
                                                      :text-transform :none
                                                      }

                                       })))


(defn- loaded-input [{:keys [value on-click classes]}]
  [outlined-input {:class-name   (:outlined-input classes)
                   :variant      :outlined
                   :value        value
                   :endAdornment (reagent/as-element [input-adornment
                                                      [icon-button {:class-name (:icon-button classes)
                                                                    :on-click   on-click}
                                                       [avatar {:class-name (:icon-button classes)
                                                                :alt        "spread" :variant "square"
                                                                :src        (arg->icon (:delete icons))}]]])}])

(defn- attributes-select [{:keys [classes id label value on-change options]}]
  [form-control {:variant    :outlined
                 :class-name (:form-control classes)}
   [input-label {:id id} label]
   [select {:label-id  id
            :value     value
            :on-change (fn [^js event]
                         (let [value (-> event .-target .-value)]
                           (when on-change
                             (on-change value))))}
    (doall
      (map (fn [option]
             ^{:key option}
             [menu-item {:value option} option])
           options))]])

(defn- error-reported [message]
  (when message
    [:div.error-reported
     [:span message]]))

(defn continuous-mcc-tree [classes]
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
        ;; main container
        [grid {:container true
               :direction :column
               :spacing   1}

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          [grid {:item true
                 :xs   6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load tree file"]]
          [grid {:item true :xs 6 :xm 6}]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; column left
          [grid {:item true :xs 6 :xm 6}
           (cond
             (and (nil? tree-file-upload-progress) (nil? tree-file))
             [button-file-upload {:id               "continuous-mcc-tree-file-upload-button"
                                  :label            "Choose a file"
                                  :class-name       (:upload-button classes)
                                  :icon             :upload
                                  :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

             (not= 1 tree-file-upload-progress)
             [linear-progress {:value      (* 100 tree-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             tree-file
             [loaded-input {:classes  classes
                            :value    tree-file
                            :on-click #(>evt [:continuous-mcc-tree/delete-tree-file])}]

             :else nil)]
          ;; column right
          [grid {:item true :xs 6 :xm 6}
           (when (nil? tree-file)
             [:<>
              [typography "When upload is complete all unique attributes will be automatically filled."]
              [typography "You can then select geographical coordinates and change other settings."]])]]

         ;; row
         [grid {:container true
                :item      true
                :direction :row
                :xs        12 :xm 12}
          ;; col left
          [grid {:item true :xs 6 :xm 6}
           [typography {:class-name (:input-label classes)} "Load trees file"]]
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
             (and (nil? trees-file-upload-progress) (nil? trees-file))
             [button-file-upload {:id               "mcc-trees-file-upload-button"
                                  :disabled?        (nil? attribute-names)
                                  :label            "Choose a file"
                                  :class-name       (:upload-button classes)
                                  :icon             :upload
                                  :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

             (not= 1 trees-file-upload-progress)
             [linear-progress {:value      (* 100 trees-file-upload-progress)
                               :variant    "determinate"
                               :class-name (:upload-progress classes)}]

             trees-file
             [loaded-input {:classes  classes
                            :value    trees-file
                            :on-click #(>evt [:continuous-mcc-tree/delete-trees-file])}]

             :else nil)]

          ;; col right
          [grid {:item true
                 :xs   6 :xm 6}
           (when (nil? trees-file)
             [:<>
              [typography "Optional: Select a file with corresponding trees distribution."]
              [typography "This file will be used to compute a density interval around the MCC tree."]])]]

         ;; row
         [grid {:container     true
                :item          true
                :direction     :column
                :xs            12 :xm 12
                :align-items   :center
                :align-content :center}
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [circular-progress {:size 100}])]

         (when attribute-names
           [:<>
            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [text-field {:label     "Name" :variant :outlined
                           :value     readable-name
                           ;; :shrink    (nil? readable-name)
                           :on-change (fn [_ value] (>evt [:continuous-mcc-tree/set-readable-name value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select y coordinate"]]

             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Select x coordinate"]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [attributes-select {:classes   classes
                                  :id        "select-latitude"
                                  :value     y-coordinate
                                  :options   attribute-names
                                  :label     "Latitude"
                                  :on-change (fn [value]
                                               (>evt [:continuous-mcc-tree/set-y-coordinate value]))}]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [attributes-select {:classes   classes
                                  :id        "select-longitude"
                                  :value     x-coordinate
                                  :options   attribute-names
                                  :label     "Longitude"
                                  :on-change (fn [value]
                                               (>evt [:continuous-mcc-tree/set-x-coordinate value]))}]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Most recent sampling date"]]
             ;; col right
             [grid {:item true :xs 6 :xm 6}
              [typography {:class-name (:input-label classes)} "Time scale"]]]

            ;; row
            [grid {:container true
                   :item      true
                   :direction :row
                   :xs        12 :xm 12}
             ;; col left
             [grid {:item true
                    :xs   6 :xm 6}
              [date-picker {:wrapperClassName (:date-picker classes)
                            :date-format      time/date-format
                            :on-change        #(>evt [:continuous-mcc-tree/set-most-recent-sampling-date %])
                            :selected         most-recent-sampling-date}]]
             ;; col right
             [grid {:item true
                    :xs   6 :xm 6}
              [amount-input {:label       "Multiplier"
                             :value       time-scale-multiplier
                             :error?      (not (nil? (:time-scale-multiplier @field-errors)))
                             :helper-text (:time-scale-multiplier @field-errors)
                             :on-change   (fn [value]
                                            (>evt [:continuous-mcc-tree/set-time-scale-multiplier value]))}]]]

            [box {:padding-top    5
                  :padding-bottom 5}
             [divider {:variant "fullWidth"}]]

            [grid {:container true
                   :direction :row
                   :spacing   1}
             [grid {:item true}
              [button {:variant   "contained"
                       :color     "primary"
                       :size      "large"
                       :disabled  (boolean (seq @field-errors))
                       :className (:start-button classes)
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
              [button {:variant   "contained"
                       :disabled  (boolean (seq @field-errors))
                       :color     "primary"
                       :size      "large"
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
             :or   {burn-in 10}}
            @bayes-factor]
        #_[:div.bayes-factor
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
                 :classes   {:indicator (:indicator classes)}
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
            "discrete-mcc-tree"   [discrete-mcc-tree classes]
            "discrete-rates"      [discrete-rates classes]
            "continuous-mcc-tree" [continuous-mcc-tree classes]
            [continuous-mcc-tree classes])]]))))
