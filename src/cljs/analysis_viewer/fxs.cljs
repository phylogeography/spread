(ns analysis-viewer.fxs
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [analysis-viewer.svg-renderer :as svg-renderer]
            [analysis-viewer.views :as views]
            [hiccups.runtime :as hiccupsrt]
            [re-frame.core :as re-frame]
            [shared.math-utils :as math-utils]))

(defn data-map [geo-json-map analysis-data time map-opts ui-params]
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
   [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill (:background-color map-opts)}]
   
   ;; map and data svg
   [:g {}
    [:svg {:view-box "0 0 360 180"}
     ;; map group
     [:g {}      
      (binding [svg-renderer/*coord-transform-fn* math-utils/map-coord->proj-coord]
        (svg-renderer/geojson->svg geo-json-map 
                                  map-opts))]

     ;; data group
     [:g {}
      (when analysis-data
        [:g {}
         (for [primitive-object analysis-data]
           ^{:key (str (:id primitive-object))}
           (views/map-primitive-object primitive-object 1 time ui-params))])]]]])

(re-frame/reg-fx
 :spread/download-current-map-as-svg
 (fn [{:keys [geo-json-map analysis-data time map-params ui-params]}]
   (let [svg-text (html (data-map geo-json-map analysis-data time map-params ui-params))
         download-anchor (js/document.createElement "a")]

     (.setAttribute download-anchor "href" (str "data:image/svg+xml;charset=utf-8," (js/encodeURIComponent svg-text)))
     (.setAttribute download-anchor "download" "map.svg")
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor))))

(def ticker-ref (atom nil))

(defn stop-ticker []
  (when-let [ticker @ticker-ref]    
    (js/clearInterval ticker)))

(re-frame/reg-fx
 :ticker/start
 (fn [{:keys [millis]}]
   ;; make sure no ticker is running before starting one
   (stop-ticker)
   
   (reset! ticker-ref
           (js/setInterval (fn []
                             (re-frame/dispatch [:ticker/tick]))
                           millis))))

(re-frame/reg-fx
 :ticker/stop
 (fn [_]
   (stop-ticker)))
