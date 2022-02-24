(ns ui.new-analysis.discrete-rates
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent.core :as r]
            [shared.components :refer [button mui-slider]]
            [ui.component.button :refer [button-file-upload]]
            [ui.component.input :refer [loaded-input text-input]]
            [ui.new-analysis.file-formats :as file-formats]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt debounce round]]))

(defn controls [{:keys [readable-name burn-in]}
                {:keys [disabled?]}]
  (let [paste-disabled? (nil? @(re-frame/subscribe [::subs/pastebin]))]
    [:div.controls-wrapper
     [:div.controls {:style {:grid-area "controls"}}
      [button {:text      "Start analysis"
               :on-click  #(>evt [:bayes-factor/start-analysis {:readable-name readable-name
                                                                :burn-in       burn-in}])
               :class     "golden"
               :disabled? disabled?}]
      [button {:text      "Paste settings"
               :on-click  #(>evt [:general/paste-analysis-settings])
               :class     "secondary"
               :disabled? paste-disabled?}]
      [button {:text      "Reset"
               :on-click  #(>evt [:bayes-factor/reset])
               :class     "danger"
               :disabled? disabled?}]]]))

(defn discrete-rates []
  (let [bayes-factor (re-frame/subscribe [::subs/bayes-factor])
        field-errors (r/atom #{})]
    (fn []
      (let [{:keys [id
                    readable-name
                    log-file-name
                    log-file-upload-progress
                    locations-file-url
                    locations-file-name
                    locations-file-upload-progress
                    burn-in]
             :or   {burn-in 0.1}}
            @bayes-factor
            controls-disabled? (or (seq? @field-errors)
                                   (not log-file-name))
            ;; weird JS behaviour, where it will parse floats with full precision
            burn-in (round burn-in 2)
            ]

        (prn "BURN-IN" burn-in)

        [:<>
         [:div.data {:style {:grid-area "data"}}
          [:section.load-log-file
           [:div
            [:h4 "Load log file"]
            (if (not log-file-name)
              (if (not (pos? log-file-upload-progress))
                [button-file-upload {:id                    "bayes-factor-log-file-upload-button"
                                     :label                 "Choose a file"
                                     :on-file-accepted      #(do
                                                          (swap! field-errors disj :log-file-error)
                                                          (>evt [:bayes-factor/on-log-file-selected %]))
                                     :file-accept-predicate file-formats/log-file-accept-predicate
                                     :on-file-rejected      (fn [] (swap! field-errors conj :log-file-error))}]
                [linear-progress {:value   (* 100 log-file-upload-progress)
                                  :variant "determinate"}])
              ;; we have a filename
              [loaded-input {:value    log-file-name
                             :on-click #(>evt [:bayes-factor/delete-log-file])}])]
           (cond
             (contains? @field-errors :log-file-error) [:div.field-error.button-error "Log file first row doesn't contain all numbers."]
             (nil? log-file-name)                      [:p.doc "Upload log file. You can then upload a matching coordinates file."])]
          [:section.load-locations-file
           [:div
            [:h4 "Load locations file"]
            (if (not locations-file-name)
              (if (not (pos? locations-file-upload-progress))
                [button-file-upload {:id                    "bayes-factor-locations-file-upload-button"
                                     :label                 "Choose a file"
                                     :on-file-accepted      #(do
                                                          (swap! field-errors disj :locations-file-error)
                                                          (>evt [:bayes-factor/on-locations-file-selected %]))
                                     :file-accept-predicate file-formats/locations-file-accept-predicate
                                     :on-file-rejected      (fn [] (swap! field-errors conj :locations-file-error))}]
                [linear-progress {:value   (* 100 locations-file-upload-progress)
                                  :variant "determinate"}])
              ;; we have a filename
              [loaded-input {:value    locations-file-name
                             :on-click #(>evt [:bayes-factor/delete-locations-file])}])]
           (cond
             (contains? @field-errors :locations-file-error) [:div.field-error.button-error "Locations file first row doesn't contain all numbers."]
             (nil? locations-file-name)                      [:p.doc "Select a file that maps geographical coordinates to the log file columns. Once this file is uploaded you can start your analysis."])]

          (when (and log-file-name
                     locations-file-name)
            [:div.field-table
             [:div.field-line
              [:div.field-card
               [:h4 "File info"]
               [text-input {:label     "Name"
                            :variant   :outlined
                            :value     readable-name
                            :on-change (fn [value]
                                         (debounce (>evt [:bayes-factor/set-readable-name value]) 10))}]]
              [:div.field-card
               [:h4 "Select burn-in"]
               [mui-slider {:step      0.1
                            :class     "speed-slider"
                            :min-val   0
                            :max-val   0.9
                            :vertical? false
                            :value     burn-in
                            :on-change (fn [_ value]
                                         (debounce (>evt [:bayes-factor/set-burn-in value]) 10))
                            :marks     true}]]]])]
         [controls {:id                 id
                    :readable-name      readable-name
                    :burn-in            burn-in
                    :locations-file-url locations-file-url}
          {:disabled? controls-disabled?}]]))))
