(ns spread.fxs
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [re-frame.core :as re-frame]
            [spread.views.maps :as views.maps]
            [spread.views.svg-renderer :as svg-renderer]
            [hiccups.runtime :as hiccupsrt]))

(defn data-map [geo-json-map data-points]
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :xmlns:amcharts "http://amcharts.com/ammap"
         :xmlns:xlink "http://www.w3.org/1999/xlink"
         :version "1.1"
         :width "100%"
         :height "100%"
         :id "map-and-data"}

   ;; gradients definitions
   [:defs {}
    [:linearGradient {:id "grad"}
     [:stop {:offset "0%" :stop-color :red}]
     [:stop {:offset "100%" :stop-color :yellow}]]]

   ;; map background
   [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill (:background-color views.maps/theme)}]
   
   ;; map and data svg
   [:g {}
    [:svg {:view-box "0 0 360 180"}
     ;; map group
     [:g {}
      (binding [svg-renderer/*theme* views.maps/theme]
        (svg-renderer/geojson->svg geo-json-map))]

     ;; data group
     [:g {}
      (for [{:keys [x1 y1 x2 y2]} data-points]
        ^{:key (str x1 y1 x2 y2)}
        (views.maps/svg-cuad-curve-data-points {:x1 x1  :y1 y1 :x2 x2 :y2 y2}))]]]])

(re-frame/reg-fx
 :spread/download-current-map-as-svg
 (fn [{:keys [geo-json-map data-points]}]   
   (let [ svg-text (html (data-map geo-json-map data-points))
         download-anchor (js/document.createElement "a")]

     (.setAttribute download-anchor "href" (str "data:image/svg+xml;charset=utf-8," (js/encodeURIComponent svg-text)))
     (.setAttribute download-anchor "download" "map.svg")
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor)
     (println "Done"))))
