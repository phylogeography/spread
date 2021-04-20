(ns analysis-viewer.views
  "Render maps and analysis data as hiccup svg vectors.
  Also handles animations."
  (:require [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.subs :as subs]
            [analysis-viewer.svg-renderer :as svg-renderer]
            [clojure.string :as str]
            [goog.string :as gstr]
            [re-frame.core :as re-frame :refer [dispatch]]
            [reagent.core :as reagent]
            [shared.math-utils :as math-utils]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The next three functions render primitive analysis output objects (points, arcs and areas) ;;
;; as svg elements                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn svg-point-object [{:keys [coord show-start show-end]} scale time-perc]
  (let [show? (<= show-start time-perc show-end)
        [x1 y1] coord]
    ;; TODO: add attrs
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:cx x1 :cy y1 :r (/ 0.4 scale) :stroke :red :fill :blue}]]))

(defn svg-area-object [{:keys [coords show-start show-end]} _ time-perc]
  (let [show? (<= show-start time-perc show-end)]
    ;; TODO: add attrs
    [:g {:style {:display (if show? :block :none)}}
     [:polygon
      {:points (->> coords
                    (map (fn [coord] (str/join " " coord)))
                    (str/join ","))
       :stroke :red
       :fill :blue
       ;; TODO: make this depend on scale
       :stroke-width "0.02"}]]))

(defn svg-quad-curve-object [{:keys [from-coord to-coord show-start show-end]} scale time-perc]
  (let [show? (<= show-start time-perc show-end)
        clip-perc (when show?
                    (/ (- time-perc show-start)
                       (- show-end show-start)))        
        [x1 y1] from-coord
        [x2 y2] to-coord
        {:keys [f1]} (math-utils/quad-curve-focuses x1 y1 x2 y2)
        [f1x f1y] f1
        curve-path-info {:d (str "M " x1 " " y1 " Q " f1x " " f1y " " x2 " " y2)
                         :stroke "url(#grad)"
                         :stroke-width (/ 0.6 scale)
                         :fill :transparent}]
    ;; TODO: add attrs
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:cx x1 :cy y1 :r (/ 0.4 scale) :stroke :red :fill :blue}] 
     [:circle {:cx x2 :cy y2 :r (/ 0.4 scale) :stroke :red :fill :blue}]
     [:path (if clip-perc

              ;; animated dashed curves
              (let [c-length (int (math-utils/quad-curve-length x1 y1 f1x f1y x2 y2))]
                (assoc curve-path-info
                       :stroke-dasharray c-length
                       :stroke-dashoffset (- c-length (* c-length clip-perc))))

              ;; normal curves
              curve-path-info)]]))

(defn map-primitive-object [{:keys [type] :as primitive-object} scale time]
  (case type
    :arc   (svg-quad-curve-object primitive-object scale time)
    :point (svg-point-object primitive-object scale time)
    :area  (svg-area-object primitive-object scale time)))

(def left-button  0)
(def wheel-button 1)
(def right-button 2)

(def theme {:map-fill-color "#424242"
            :background-color "#292929"
            :map-stroke-color "pink"
            :map-text-color "pink"
            :line-color "orange"
            :data-point-color "#00ffa5"})

(def animation-delta-t 400)
(def animation-increment 0.04)

(defn controls [{:keys [dec-time-fn inc-time-fn time]}]
  [:div
   [:button {:on-click dec-time-fn} "<"]
   [:button {:on-click inc-time-fn} ">"]
   [:button {:on-click (fn next-anim-step []
                         (js/setTimeout
                          (fn [] (when (< @time 1)
                                   (inc-time-fn)
                                   (next-anim-step)))
                          animation-delta-t))} "Play"]
   [:span (str (int (* 100 @time)) "%")]
   [:button {:on-click #(dispatch [:map/toggle-show-world])}
    "Hide/Show World map"]
   [:button {:on-click #(dispatch [:map/download-current-as-svg @time])}
    "Download"]])

;; TODO: refactor this component and its subscriptions for performance
(defn animated-data-map []
  (let [time (reagent/atom 0)
        inct (fn [] (if (< @time (- 1 animation-increment))
                      (swap! time #(+ % animation-increment))
                      (reset! time 1)))
        dect (fn [] (if (> @time animation-increment)
                      (swap! time #(- % animation-increment))
                      (reset! time 0)))]
    (fn []      
      (let [t            @time
            geo-json-map @(re-frame/subscribe [::subs/map-data])
            analysis-data  @(re-frame/subscribe [::subs/analysis-data])
            {:keys [grab translate scale zoom-rectangle]} @(re-frame/subscribe [::subs/map-state])
            scale (or scale 1)
            [translate-x translate-y] translate]
        [:div {:style {:height (str events.maps/map-screen-height "px")
                       :width  (str events.maps/map-screen-width  "px")}
               :on-wheel (fn [evt]
                           (let [x (-> evt .-nativeEvent .-offsetX)
                                 y (-> evt .-nativeEvent .-offsetY)]
                             (dispatch [:map/zoom {:delta (.-deltaY evt)
                                                   :x x
                                                   :y y}])))
               :on-mouse-down (fn [evt]
                                (.stopPropagation evt)
                                (.preventDefault evt)
                                (let [x (-> evt .-nativeEvent .-offsetX)
                                      y (-> evt .-nativeEvent .-offsetY)]
                                  (cond

                                    ;; left button pressed
                                    (= left-button (.-button evt))                                  
                                    (dispatch [:map/grab {:x x :y y}])

                                    ;; wheel button pressed
                                    (= wheel-button (.-button evt))
                                    (dispatch [:map/zoom-rectangle-grab {:x x :y y}]))))
               
               :on-mouse-move (fn [evt]
                                (let [x (-> evt .-nativeEvent .-offsetX)
                                      y (-> evt .-nativeEvent .-offsetY)]
                                  (cond
                                    grab
                                    (dispatch [:map/drag {:x x :y y}])

                                    zoom-rectangle
                                    (dispatch [:map/zoom-rectangle-update {:x x :y y}]))))
               :on-mouse-up (fn [evt]
                              (.stopPropagation evt)
                              (.preventDefault evt)
                              (cond
                                (= left-button (.-button evt))
                                (dispatch [:map/grab-release])

                                (= wheel-button (.-button evt))
                                (dispatch [:map/zoom-rectangle-release])))}
         
         ;; Controls
         [controls {:dec-time-fn dect
                    :inc-time-fn inct
                    :time time}]

         ;; SVG data map
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
          [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill (:background-color theme)}]
          
          ;; map and data svg
          [:g {:transform (gstr/format "translate(%f %f) scale(%f %f)"
                                       (or translate-x 0)
                                       (or translate-y 0)
                                       scale scale)}
           [:svg {:view-box "0 0 360 180"}
            ;; map group
            (when geo-json-map
              [:g {}
               (binding [svg-renderer/*theme* theme]
                 (svg-renderer/geojson->svg geo-json-map))])

            ;; data group
            (when analysis-data
              [:g {}
               (for [primitive-object analysis-data]
                 ^{:key (str (:id primitive-object))}
                 [map-primitive-object primitive-object scale t])])]]

          (when zoom-rectangle
            (let [[x1 y1] (:origin zoom-rectangle)
                  [x2 y2] (:current zoom-rectangle)]
              [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke (:data-point-color theme) :fill :transparent}]))]])
      
      )))


