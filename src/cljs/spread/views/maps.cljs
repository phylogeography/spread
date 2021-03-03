(ns spread.views.maps
  (:require [spread.math-utils :as math-utils]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [spread.subs :as subs]))

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

(defn animated-data []
  (let [time (reagent/atom 0)
        dt 0.1
        inct (fn [] (if (< @time (- 1 dt)) (swap! time #(+ % dt)) (reset! time 1)))
        dect (fn [] (if (> @time dt) (swap! time #(- % dt)) (reset! time 0)))
        maps (re-frame/subscribe [::subs/maps])]
    (fn []
      [:div {}
       [spread.views.svg-renderer/svg-map @maps]
       
       #_[:div
        [:button {:on-click dect} "<"]
        [:button {:on-click inct} ">"]
        [:button {:on-click (fn next-anim-step []
                              (js/setTimeout
                               (fn [] (when (< @time (- 1 dt))
                                        (inct)
                                        (next-anim-step)))
                               80))} "Play"]
        #_[:span @time]]
       #_[:svg {:width "1000"
              :height "1000"
              :xmlns "http://www.w3.org/2000/svg"}
        [:defs {}
         [:linearGradient {:id "grad"}
          [:stop {:offset "0%" :stop-color :red}]
          [:stop {:offset "100%" :stop-color :yellow}]]]
        [clipped-svg-cuad-curve {:x1 10  :y1  10 :x2 100 :y2 100 :clip-perc @time}]
        [clipped-svg-cuad-curve {:x1 500 :y1 450 :x2 700 :y2 300 :clip-perc @time}]
        [clipped-svg-cuad-curve {:x1 300 :y1 445 :x2 250 :y2 300 :clip-perc @time}]]])))

