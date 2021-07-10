(ns analysis-viewer.fxs
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [analysis-viewer.map-emitter :as map-emitter]
            [analysis-viewer.svg-renderer :as svg-renderer]            
            [analysis-viewer.views :as views]
            [goog.string :as gstr]
            [hiccups.runtime :as hiccupsrt]
            [re-frame.core :as re-frame]
            [shared.math-utils :as math-utils]))

(defn data-map [geo-json-map analysis-data time params]  
  (let [analysis-data-box (map-emitter/data-box analysis-data)
        padding 10]
    [:svg {:xmlns "http://www.w3.org/2000/svg"
          :xmlns:amcharts "http://amcharts.com/ammap"
          :xmlns:xlink "http://www.w3.org/1999/xlink"
          :version "1.1"
          :width "1000px"
          :height "500px"
          :id "map-and-data"
          :preserve-aspect-ratio "xMinYMin"
          :viewBox (gstr/format "%d %d %d %d"
                                (- (:min-x analysis-data-box) padding)
                                (- (:min-y analysis-data-box) padding)
                                (+ (* 2 padding) (- (:max-x analysis-data-box) (:min-x analysis-data-box)))
                                (+ (* 2 padding) (- (:max-y analysis-data-box) (:min-y analysis-data-box))))}
    
    ;; map background
    [:rect {:x "0" :y "0" :width "360" :height "180" :fill (:background-color params)}]    
    
    ;; map group
    [:g {}
     [:g {}      
      (binding [svg-renderer/*coord-transform-fn* math-utils/map-coord->proj-coord]
        (svg-renderer/geojson->svg geo-json-map 
                                   (assoc params :clip-box analysis-data-box)))]

     ;; data group     
     [:g {}
      (when analysis-data
        [:g {}
         (for [primitive-object analysis-data]
           ^{:key (str (:id primitive-object))}
           (views/map-primitive-object primitive-object 1 time params))])]

     ;; text group
     (views/text-group analysis-data time params)]]))

(re-frame/reg-fx
 :spread/download-current-map-as-svg
 (fn [{:keys [geo-json-map analysis-data time params]}]
   (let [svg-text (html (data-map geo-json-map analysis-data time params))
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
