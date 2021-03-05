(ns spread.views.maps
  (:require [spread.math-utils :as math-utils]
            [spread.views.svg-renderer :as svg-renderer]
            [spread.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [dispatch]]
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

(def left-button  1)
(def right-button 2)

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
        dect (fn [] (if (> @time dt) (swap! time #(- % dt)) (reset! time 0)))]
    (fn []
      (let [geo-json-map @(re-frame/subscribe [::subs/map-data])
            view-box     @(re-frame/subscribe [::subs/map-view-box])
            data-points  @(re-frame/subscribe [::subs/data-points])
            map-grab     @(re-frame/subscribe [::subs/map-grab])
            t            @time]
        [:div {:style {:height (str events/map-height "px")
                       :width  (str events/map-width  "px")}
               :on-wheel (fn [evt]
                           (let [x (-> evt .-nativeEvent .-offsetX)
                                 y (-> evt .-nativeEvent .-offsetY)]
                             ;; send x,y in world projection coordinates
                             (dispatch [::events/zoom {:in? (neg? (.-deltaY evt))
                                                       :x x
                                                       :y y}])))
               :on-mouse-down (fn [evt]
                                (when (= left-button (.-buttons evt))                                  
                                  (let [x (-> evt .-nativeEvent .-offsetX)
                                        y (-> evt .-nativeEvent .-offsetY)]
                                    (.stopPropagation evt)
                                    (.preventDefault evt)
                                    (dispatch [::events/map-grab {:x x :y y}]))))
               
               :on-mouse-move (fn [evt]
                                (when map-grab
                                  (let [x (-> evt .-nativeEvent .-offsetX)
                                        y (-> evt .-nativeEvent .-offsetY)]
                                    (dispatch [::events/map-drag {:x x :y y}]))))
               :on-mouse-up (fn [evt]
                              (dispatch [::events/map-grab-release]))
               }
         
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
         (when (and view-box)
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

           ;; map and data svg
           [:svg {:viewBox (gstr/format "%f %f %f %f" (:x view-box) (:y view-box) (:w view-box) (:h view-box))}
            ;; map group
            [:g {}
             (binding [svg-renderer/*theme* theme]
               (svg-renderer/geojson->svg geo-json-map))]

            ;; data group
            [:g {}
             (for [{:keys [x1 y1 x2 y2]} data-points]
               ^{:key (str x1 y1 x2 y2)}
               [clipped-svg-cuad-curve {:x1 x1  :y1 y1 :x2 x2 :y2 y2 :clip-perc t}])]]])])
      
      )))

