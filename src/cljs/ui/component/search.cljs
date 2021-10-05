(ns ui.component.search
  (:require [reagent.core :as reagent]))

(defn search-bar [_]  
  (let [val (reagent/atom "")]
    (fn [{:keys [on-change placeholder]}]
      [:div.search-bar
       [:input {:type :text
                :placeholder placeholder
                :value @val
                :on-change (fn [e]
                             (let [v (-> e .-target .-value)]
                               (reset! val v)                               
                               (on-change v)))}]
       (if (empty? @val)
         [:i.zmdi.zmdi-search]
         [:i.zmdi.zmdi-close {:on-click #(do
                                           (reset! val "")
                                           (on-change ""))}])])))
