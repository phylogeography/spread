(ns ui.component.input)

(defn text-input
  [{:keys [on-change value class]}]
  [:input {:type :text
           :class class
           :value value
           :on-change (fn [^js event]
                        (let [value (-> event .-target .-value)]
                          (on-change value)))}])
