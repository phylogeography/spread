(ns spread.views.maps
  (:require [spread.math-utils :as math-utils]
            [spread.views.svg-renderer :as svg-renderer]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [spread.subs :as subs]
            [goog.string :as gstr]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)

(defn clipped-svg-cuad-curve [{:keys [x1 y1 x2 y2 clip-perc]}]
  (let [{:keys [f1 f2]} (math-utils/cuad-curve-focuses x1 y1 x2 y2)
        [f1x f1y] f1
        [f2x f2y] f2 
        l-length (math-utils/sqrt (+ (math-utils/pow (- x2 x1) 2)
                                     (math-utils/pow (- y2 y1) 2)))
        c-length (int (math-utils/cuad-curve-length x1 y1 f1x f1y x2 y2))]
    [:g {}
     [:circle {:cx x1 :cy y1 :r 2 :stroke :red :fill :red}] 
     [:circle {:cx x2 :cy y2 :r 2 :stroke :red :fill :red}]
     [:path {:d (str "M " x1 " " y1 " Q " f1x " " f1y " " x2 " " y2)
             :stroke "url(#grad)"
             :stroke-dasharray c-length
             :stroke-dashoffset (+ c-length (* c-length clip-perc))
             :fill :transparent}]
     ;; enable for debugging
     #_[:g {}
        [:circle {:cx f1x :cy f1y :r 2 :stroke :green :fill :green}]
        [:circle {:cx f2x :cy f2y :r 2 :stroke :green :fill :green}]
        [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke :blue}]      
        [:path {:d (str "M " x1 " " y1 " Q " f2x " " f2y " " x2 " " y2)
                :stroke :black
                :stroke-dasharray 5
                :stroke-dashoffset 0
                :fill :transparent}]
        
        [:text {:x x1 :y y1 :stroke :blue} (str (int l-length))]
        [:text {:x f1x :y f1y :stroke :blue} (str (int c-length))]]]))



(defn view-box-bounding-box

  "Calculates a screen bounding box (in screen coordinates) given a `map-box` (in long, lat)
  adding `padding` around."
  
  [map-box padding]

  (let [[x1 y1] (svg-renderer/map-coord->screen-coord [(:min-long map-box) (:min-lat map-box)])
        [x2 y2] (svg-renderer/map-coord->screen-coord [(:max-long map-box) (:max-lat map-box)])]
    [(- (min x1 x2) (/ padding 2))
     (- (min y1 y2) (/ padding 2))
     (+ (Math/abs (- x1 x2)) padding)
     (+ (Math/abs (- y1 y2)) padding)]))

(defn animated-data-map []
  (let [theme {:map-fill-color "#424242"
               :background-color "#292929"
               :map-stroke-color "pink"
               :map-text-color "pink"
               :line-color "orange"
               :data-point-color "#00ffa5"}
        time (reagent/atom 0)
        dt 0.1
        inct (fn [] (if (< @time (- 1 dt)) (swap! time #(+ % dt)) (reset! time 1)))
        dect (fn [] (if (> @time dt) (swap! time #(- % dt)) (reset! time 0)))
        maps (re-frame/subscribe [::subs/maps])
        data [{:x1 104  :y1  110 :x2 120 :y2 130}
              #_{:x1 500 :y1 450 :x2 700 :y2 300}
              #_{:x1 300 :y1 445 :x2 250 :y2 300}]]
    (fn []
      (let [geo-json-map @maps]
        [:div {:style {:height "600px"}}
         
         ;; Controls
         [:div
          [:button {:on-click dect} "<"]
          [:button {:on-click inct} ">"]
          [:button {:on-click (fn next-anim-step []
                                (js/setTimeout
                                 (fn [] (when (< @time (- 1 dt))
                                          (inct)
                                          (next-anim-step)))
                                 80))} "Play"]
          [:span @time]]

         ;; SVG data map
         [:svg {:xmlns "http://www.w3.org/2000/svg"
                :xmlns:amcharts "http://amcharts.com/ammap"
                :xmlns:xlink "http://www.w3.org/1999/xlink"
                :version "1.1"
                :width "100%"
                :height "100%"}

          ;; gradients definitions
          [:defs {}
           [:linearGradient {:id "grad"}
            [:stop {:offset "0%" :stop-color :red}]
            [:stop {:offset "100%" :stop-color :yellow}]]]

          ;; map background
          [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill (:background-color theme)}]

          ;; map and data svg,                 x  y  w  h
          [:svg {:viewBox (apply gstr/format "%f %f %f %f" (view-box-bounding-box (:map-box geo-json-map)
                                                                                  data-box-padding))}
           ;; map group
           [:g {}
            (binding [svg-renderer/*theme* theme]
              (svg-renderer/geojson->svg geo-json-map))]

           ;; data group
           [:g {}
            (for [{:keys [x1 y1 x2 y2]} data]
              [clipped-svg-cuad-curve {:x1 x1  :y1 y1 :x2 x2 :y2 y2 :clip-perc @time}])]]]])
      
      )))

