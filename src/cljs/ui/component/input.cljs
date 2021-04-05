(ns ui.component.input
  (:require [clojure.set :refer [rename-keys]]))

(defn text-input
  [{:keys [on-change value class]}]
  [:input {:type :text
           :class class
           :value value
           :on-change (fn [^js event]
                        (let [value (-> event .-target .-value)]
                          (on-change value)))}])

#_(defn select-input [{:keys [value on-change options]}]
  [:select {:on-change (fn [^js item]
                         (let [val (-> item .-target .-value)
                               iv (and (re-matches #"^\d*(\.|\.)?\d*$" val)
                                       (js/parseFloat val))
                               val (if
                                       (or
                                        (nil? iv)
                                        (js/isNaN iv))
                                     val
                                     iv)
                               label (-> (.-target.selectedOptions item)
                                         (aget 0)
                                         .-innerHTML)]

                           (prn {:label label
                                 :value val})

                           (when on-change
                             (on-change {:value val :label label}))

                           ))
            :value value}

   (doall
    (map (fn [option]
           ^{:key (str (:key option))}
           [:option (rename-keys option {:key :value})
            (:value option)])
         options))])


(defn select-input
  [{:keys [value  options on-change class]}]
  [:select {:class class
            :value value
            :on-change (fn [^js event]
                         (let [value (-> event .-target .-value)
                               value (if (re-matches #"^\d*(\.|\.)?\d*$" value)
                                       (js/parseFloat value)
                                       value)]
                           (when on-change
                             (on-change value))))}
   (doall
    (for [option options]
      [:option {:key option} option]))])
