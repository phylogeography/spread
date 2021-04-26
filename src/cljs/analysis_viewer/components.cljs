(ns analysis-viewer.components
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn switch-button [{:keys [id]}]
  (let [on? @(subscribe [:switch-buttons/on? id])]
    [:div.switch-button {:class (if on? "on" "off")
                         :on-click (fn [evt]
                                     (.stopPropagation evt)
                                     (dispatch [:switch-button/toggle id]))}
     [:span.on "On"]
     [:span.block {:class (if on? "block-on" "block-off")}]
     [:span.off "Off"]]))

(defn slider [{:keys [zoom-perc zoom-inc-fn zoom-dec-fn class]}]
  [:div.slider {:class class}
   [:button {:on-click zoom-inc-fn} "+"]
   [:div {:width "6px" :height "100%"}
    [:svg 
     [:line {:x1 "10" :y1 "100" :x2 "10" :y2 "0" :stroke "#DEDEE8" :stroke-width 3}]
     [:line {:x1 "10" :y1 "100" :x2 "10" :y2 "0" :stroke "#EEBE53" :stroke-width 3
             :stroke-dasharray 100
             :stroke-dashoffset zoom-perc}]
     [:rect {:x "4" :y (str (- zoom-perc 6)) :width "12" :height "12" :fill "white" :stroke "grey"}]]]
   [:button {:on-click zoom-dec-fn} "-"]])
