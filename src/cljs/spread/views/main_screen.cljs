(ns spread.views.main-screen
  (:require [spread.views.maps :as views.maps]))

(defn main-panel []
  [:div {} "Main panel"
   [views.maps/animated-data-map]])
