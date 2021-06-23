(ns ui.component.select
  (:require [reagent-material-ui.core.form-control :refer [form-control]]
            [reagent-material-ui.core.input-label :refer [input-label]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.select :refer [select]]))

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
