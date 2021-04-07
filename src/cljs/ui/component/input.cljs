(ns ui.component.input)

(defn text-input
  [{:keys [on-change value class]}]
  [:input {:type      :text
           :class     class
           :value     value
           :on-change (fn [^js event]
                        (let [value (-> event .-target .-value)]
                          (on-change value)))}])

(defn amount-input [{:keys [class value on-change]}]
  [:input {:type      :text
           :class     class
           :value     value
           :on-change (fn [^js event]

                        (let [value (-> event .-target .-value)
                              value (if (re-matches #"^\d*(\.|\.)?\d*$" value)
                                      (js/parseFloat value)
                                      value)
                              value (if (js/isNaN value) nil value)]
                          (when on-change
                            (on-change value))))}])

;; TODO : make them searchable
(defn select-input
  [{:keys [value options on-change class]}]
  [:select {:class     class
            :value     value
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
