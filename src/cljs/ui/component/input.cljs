(ns ui.component.input
  (:require [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.input-adornment :refer [input-adornment]]
            [reagent-material-ui.core.outlined-input :refer [outlined-input]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent.core :as reagent]
            [ui.component.icon :refer [arg->icon icons]]))

(defn amount-input [{:keys [value error? helper-text on-change label]}]
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

(defn loaded-input [{:keys [value on-click classes]}]
  [outlined-input {:class-name   (:outlined-input classes)
                   :variant      :outlined
                   :value        value
                   :endAdornment (reagent/as-element [input-adornment
                                                      [icon-button {:class-name (:icon-button classes)
                                                                    :on-click   on-click}
                                                       [avatar {:class-name (:icon-button classes)
                                                                :alt        "spread" :variant "square"
                                                                :src        (arg->icon (:delete icons))}]]])}])

(defn text-input [{:keys [value error? helper-text on-change label]}]
  [text-field {:label      label
               :variant    :outlined
               :value      value
               :error      error?
               :helperText helper-text
               :on-change  (fn [^js event]
                             (let [value (-> event .-target .-value)]
                               (when on-change
                                 (on-change value))))}])
