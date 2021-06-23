(ns ui.component.input
  (:require [reagent-material-ui.core.text-field :refer [text-field]]))

(defn text-input
  [{:keys [on-change value class read-only?]}]
  [:input {:type      :text
           :readOnly  read-only?
           :class     class
           :value     value
           :on-change (fn [^js event]
                        (when on-change
                          (let [value (-> event .-target .-value)]
                            (on-change value))))}])

(defn amount-input [{:keys [classes value error? helper-text on-change label]}]
  [text-field {:label      label
               :variant    :outlined
               :value      value
               :error      error?
               :helperText helper-text
               :on-change  (fn [^js event]
                             (let [value (-> event .-target .-value)
                                   value (if (re-matches #"^\d*(\.|\.)?\d*$" value)
                                           (js/parseFloat value)
                                           value)
                                   value (if (js/isNaN value) nil value)]
                               (when on-change
                                 (on-change value))))}])

;; TODO : make them searchable
;; (defn select-input
;;   [{:keys [value options on-change class]}]
;;   [:select {:class     class
;;             :value     value
;;             :on-change (fn [^js event]
;;                          (let [value (-> event .-target .-value)
;;                                value (if (re-matches #"^\d*(\.|\.)?\d*$" value)
;;                                        (js/parseFloat value)
;;                                        value)]
;;                            (when on-change
;;                              (on-change value))))}
;;    (doall
;;      (for [option options]
;;        [:option {:key option} option]))])

;; (defn range-input
;;   [{:keys [value min max on-change class]}]
;;   [:input {:type      :range
;;            :class     class
;;            :value     value
;;            :min       min
;;            :max       max
;;            :on-change (fn [^js event]
;;                         (let [value (-> event .-target .-value)]
;;                           (on-change value)))}])
