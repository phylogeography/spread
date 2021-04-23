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
