(ns analysis-viewer.views
  "Render maps and analysis data as hiccup svg vectors.
  Also handles animations."
  (:require [analysis-viewer.subs :as subs]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.components :refer [switch-button slider]]
            [analysis-viewer.svg-renderer :as svg-renderer]
            [clojure.string :as str]
            [goog.string :as gstr]
            [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [shared.math-utils :as math-utils]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The next three functions render primitive analysis output objects (points, arcs and areas) ;;
;; as svg elements                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn svg-point-object [{:keys [coord show-start show-end]} scale time-perc params]
  (let [show? (<= show-start time-perc show-end)
        [x1 y1] coord]
    ;; TODO: add attrs
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:cx x1 :cy y1 :r 0.3 #_(/ 0.4 scale) :stroke "#DD0808" :fill "#B20707"}]]))

(defn svg-area-object [{:keys [coords show-start show-end]} _ time-perc params]
  (let [show? (<= show-start time-perc show-end)]
    ;; TODO: add attrs
    [:g {:style {:display (if show? :block :none)}}
     [:polygon
      {:points (->> coords
                    (map (fn [coord] (str/join " " coord)))
                    (str/join ","))
       
       :fill "#9E15E6"
       :opacity (:polygon-opacity params)}]]))

(defn svg-quad-curve-object [{:keys [from-coord to-coord show-start show-end]} scale time-perc params]
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
     [:circle {:cx x1 :cy y1 :r 0.3 #_(/ 0.4 scale) :stroke :red :fill :blue}] 
     [:circle {:cx x2 :cy y2 :r 0.3 #_(/ 0.4 scale) :stroke :red :fill :blue}]
     [:path (if clip-perc

              ;; animated dashed curves
              (let [c-length (int (math-utils/quad-curve-length x1 y1 f1x f1y x2 y2))]
                (assoc curve-path-info
                       :stroke-dasharray c-length
                       :stroke-dashoffset (- c-length (* c-length clip-perc))))

              ;; normal curves
              curve-path-info)]]))

(defn map-primitive-object [{:keys [type] :as primitive-object} scale time params]
  (case type
    :arc   (svg-quad-curve-object primitive-object scale time params)
    :point (svg-point-object primitive-object scale time params)
    :area  (svg-area-object primitive-object scale time params)))

(def left-button  0)
(def wheel-button 1)
(def right-button 2)

(def animation-delta-t 50)
(def animation-increment 0.02)



(defn animation-controls [{:keys [dec-time-fn inc-time-fn time-ref]}]
  (let [time @(subscribe [:animation/percentage])
        playing? (= :play @(subscribe [:animation/state]))
        ticks-data @(subscribe [:analysis/data-timeline])
        zoom-perc 50
        ticks-y-base 80
        ticks-bars-y-base (- ticks-y-base 11)
        ticks-bars-full 50                
        full-length (apply max (map :x ticks-data))
        play-line-x (* time full-length)]
    [:div.animation-controls
     [slider {:inc-buttons 0.8
              :min-val events.maps/min-scale
              :max-val events.maps/max-scale
              :length 100
              :vertical? true
              :subs-vec [:map/scale]
              :ev-vec [:map/zoom 100 100]}]
     [:div.inner
      [:div.buttons
       [:i.zmdi.zmdi-skip-previous {:on-click #(dispatch [:animation/prev])} ""]    
       [:i.zmdi {:on-click #(dispatch [:animation/toggle-play-stop])
                           :class (if playing? "zmdi-pause" "zmdi-play")}]
       [:i.zmdi.zmdi-skip-next {:on-click #(dispatch [:animation/next])} ""]]
      [:div.timeline {:width "100%" :height "100%"}
       [:svg {:width "100%" :height "100px"}
        [:g
         [:line {:x1 play-line-x :y1 0 
                 :x2 play-line-x :y2 100
                 :stroke "#EEBE53"
                 :stroke-width 2}]
         (for [{:keys [label x type perc]} ticks-data]
           ^{:key (str x)}
           [:g
            [:line {:x1 x :y1 ticks-y-base
                    :x2 x :y2 (- ticks-y-base (if (= type :short) 5 10))
                    :stroke "#3A3668"}]
            (when (pos? perc)
              [:line {:x1 x :y1 ticks-bars-y-base
                    :x2 x :y2 (- ticks-bars-y-base (/ (* perc ticks-bars-full) 100))
                      :stroke "red"}])
            (when label
              [:text {:x x :y (+ ticks-y-base 10) :font-size 10 :fill "#3A3668" :stroke :transparent :text-anchor :middle}
               label])])]]]]]))

(defn map-group []
  (let [geo-json-map @(re-frame/subscribe [:map/data])
        map-options @(re-frame/subscribe [:map/parameters])]    
    (when geo-json-map
      [:g {}
       (svg-renderer/geojson->svg geo-json-map map-options)])))

(defn data-group []
  (let [time @(re-frame/subscribe [:animation/percentage])
        analysis-data  @(re-frame/subscribe [:analysis/data])
        {:keys [scale]} @(re-frame/subscribe [:map/state])
        params @(subscribe [:ui/parameters])]
    (when analysis-data
      [:g {}
       (for [primitive-object analysis-data]
         ^{:key (str (:id primitive-object))}
         [map-primitive-object primitive-object scale time params])])))

(defn animated-data-map []
  (fn []      
    (let [{:keys [grab translate scale zoom-rectangle]} @(re-frame/subscribe [:map/state])
          scale (or scale 1)
          [translate-x translate-y] translate]
      [:div.animated-data-map
       [:div.map-wrapper {:style {} #_{:height (str events.maps/map-screen-height "px")
                                       :width  (str events.maps/map-screen-width  "px")}
                          :on-wheel (fn [evt]
                                      (let [x (-> evt .-nativeEvent .-offsetX)
                                            y (-> evt .-nativeEvent .-offsetY)]
                                        (dispatch [:map/zoom-inc {:delta (.-deltaY evt)
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
        [:div.zoom-bar-outer
         [:div.zoom-bar-back]
         [slider {:inc-buttons 0.8
                  :min-val events.maps/min-scale
                  :max-val events.maps/max-scale
                  :length 100
                  :vertical? true
                  :subs-vec [:map/scale]
                  :ev-vec [:map/zoom 100 100]
                  :class "map-zoom"}]]
        
        ;; SVG data map
        [:svg {:xmlns "http://www.w3.org/2000/svg"
               :xmlns:amcharts "http://amcharts.com/ammap"
               :xmlnsXlink "http://www.w3.org/1999/xlink"
               :version "1.1"
               :width "100%"
               :height "100%"
               :id "map-and-data"}

         ;; gradients definitions
         [:defs {}
          [:linearGradient {:id "grad"}
           [:stop {:offset "0%" :stop-color "#DD0808"}]
           [:stop {:offset "100%" :stop-color "#B20707"}]]]

         ;; map background
         [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill "#ECEFF8"}]
         
         ;; map and data svg
         [:g {:transform (gstr/format "translate(%f %f) scale(%f %f)"
                                      (or translate-x 0)
                                      (or translate-y 0)
                                      scale scale)}
          [:svg {:view-box "0 0 360 180"}
           [map-group]
           [data-group]]]

         (when zoom-rectangle
           (let [[x1 y1] (:origin zoom-rectangle)
                 [x2 y2] (:current zoom-rectangle)]
             [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke "#DD0808" :fill :transparent}]))]]
       [animation-controls]])))

(defn top-bar []
  [:div.top-bar
   [:div.logo
    [:div.logo-img
     [:div.hex.hex1] [:div.hex.hex2] [:div.hex.hex3] [:div.hex.hex4] ]
    [:span.text "spread"]]])

(defn collapsible-tab [parent-id {:keys [id title child]}]
  (let [open? @(subscribe [:collapsible-tabs/open? parent-id id])]
    [:div.tab 
     [:div.title {:on-click #(dispatch [:collapsible-tabs/toggle parent-id id])}
      [:span.text title] [:span.arrow (if open? "▲" "▼")]]
     [:div.tab-body {:class (if open? "open" "collapsed")}
      child]]))

(defn collapsible-tabs [{:keys [id title childs]}]
  [:div.collapsible-tabs
   [:div.title title]
   [:div.tabs
    (for [c childs]
      ^{:key (str (:id c))}
      [collapsible-tab id c])]])

(defn layer-visibility []
  [:div.layer-visibility
   [:label "Borders"]    [switch-button {:id :map-borders}]
   [:label "Directions"] [switch-button {:id :directions}]
   [:label "Radius"]     [switch-button {:id :radius}]])

(defn map-color-chooser []
  (let [colors ["#079DAB" "#3428CA" "#757295" "#DD0808" "#EEBE53" "#B20707" "#ECEFF8" "#FBFCFE" "#DEDEE9"
                "#266C08" "#C76503" "#1C58D0" "#A5387B" "#1C58D9" "#3A3668" "#757299" "#DEDEE8" "#3428CB"]
        selected-color @(subscribe [:parameters/selected :map-borders-color])]
    [:div.map-color-chooser
     [:div.colors
      (for [c colors]
        ^{:key c}
        [:div.color {:style {:background-color c}
                     :on-click (fn [evt]
                                 (.stopPropagation evt)
                                 (dispatch [:parameters/select :map-borders-color c]))}
         (when (= selected-color c)
           [:i.zmdi.zmdi-check])
         ])]]))

(defn polygon-opacity []
  [:div.polygon-opacity
   [slider {:inc-buttons 0.1
            :min-val 0
            :max-val 1
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :polygon-opacity]
            :ev-vec [:parameters/select :polygon-opacity]}]
   ])

(defn controls-side-bar []
  [:div.side-bar
   [:div.tabs
    [collapsible-tabs {:title "Parameters"
                       :id :parameters
                       :childs [{:title "Layer visibility"
                                 :id :layer-visibility
                                 :child [layer-visibility]}
                                {:title "Map Color"
                                 :id :map-color
                                 :child [map-color-chooser]}
                                {:title "Polygon opacity"
                                 :id :polygon-opacity
                                 :child [polygon-opacity]}]}]
    [collapsible-tabs {:title "Filters"
                       :id :filters
                       :childs [{:title "States prob"
                                 :id :states-prob
                                 :child [:div "BALBALBALB"]}
                                {:title "Node Name"
                                 :id :node-name
                                 :child [:div "UUUUUUUUU"]}]}]]
   [:div.export-panel
    [:button.export {:on-click #(dispatch [:map/download-current-as-svg])}
     "Export results"]]])

(defn main-screen []
  (fn []
    [:div.main-screen
     [top-bar]
     [controls-side-bar]
     [animated-data-map]]))
