(ns analysis-viewer.views
  "Render maps and analysis data as hiccup svg vectors.
  Also handles animations."
  (:require [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.subs :as subs]
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

(def theme {:map-fill-color "#ffffff"
            :background-color "#ECEFF8"
            :map-stroke-color "#079DAB"
            :map-text-color "#079DAB"
            :line-color "#B20707"
            :data-point-color "#DD0808"})

(def animation-delta-t 50)
(def animation-increment 0.02)

(defn animation-controls [{:keys [dec-time-fn inc-time-fn time-ref]}]
  [:div.animation-controls
   [:button {:on-click dec-time-fn} "<"]
   [:button {:on-click inc-time-fn} ">"]
   [:button {:on-click (fn next-anim-step []
                         (js/setTimeout
                          (fn [] (when (< @time-ref 1)
                                   (inc-time-fn)
                                   (next-anim-step)))
                          animation-delta-t))} "Play"]
   [:span (str (int (* 100 @time-ref)) "%")]])

(defn map-group []
  (let [geo-json-map @(re-frame/subscribe [::subs/map-data])]
    (when geo-json-map
      [:g {}
       (binding [svg-renderer/*theme* theme]
         (svg-renderer/geojson->svg geo-json-map))])))

(defn data-group [time]
  (let [analysis-data  @(re-frame/subscribe [::subs/analysis-data])
        {:keys [scale]} @(re-frame/subscribe [::subs/map-state])]
    (when analysis-data
      [:g {}
       (for [primitive-object analysis-data]
         ^{:key (str (:id primitive-object))}
         [map-primitive-object primitive-object scale time])])))

(defn animated-data-map [time-ref]
  (let [inct (fn [] (if (< @time-ref (- 1 animation-increment))
                      (swap! time-ref #(+ % animation-increment))
                      (reset! time-ref 1)))
        dect (fn [] (if (> @time-ref animation-increment)
                      (swap! time-ref #(- % animation-increment))
                      (reset! time-ref 0)))]
    (fn []      
      (let [{:keys [grab translate scale zoom-rectangle]} @(re-frame/subscribe [::subs/map-state])
            scale (or scale 1)
            [translate-x translate-y] translate]
        [:div.animated-data-map {:style {} #_{:height (str events.maps/map-screen-height "px")
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
            [map-group]
            [data-group @time-ref]]]

          (when zoom-rectangle
            (let [[x1 y1] (:origin zoom-rectangle)
                  [x2 y2] (:current zoom-rectangle)]
              [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke (:data-point-color theme) :fill :transparent}]))]
         [animation-controls {:dec-time-fn dect
                              :inc-time-fn inct
                              :time-ref time-ref}]])
      
      )))

(defn top-bar []
  [:div.top-bar
   [:div.logo
    [:div.logo-img
     [:div.hex.hex1] [:div.hex.hex2] [:div.hex.hex3] [:div.hex.hex4] ]
    [:span.text "spread"]]])

(defn collapsible-tab [parent-id {:keys [id title child]}]
  (let [open? @(subscribe [:collapsible-tabs/open? parent-id id])]
    [:div.tab {:on-click #(dispatch [:collapsible-tabs/toggle parent-id id])}
     [:div.title [:span.text title] [:span.arrow (if open? "▲" "▼")]]
     [:div.tab-body {:class (if open? "open" "collapsed")}
      child]]))

(defn collapsible-tabs [{:keys [id title childs]}]
  [:div.collapsible-tabs
   [:div.title title]
   [:div.tabs
    (for [c childs]
      ^{:key (str (:id c))}
      [collapsible-tab id c])]])

(defn controls-side-bar [time]
  [:div.side-bar
   [:div.tabs
    [collapsible-tabs {:title "Parameters"
                       :id :parameters
                       :childs [{:title "Layer visibility"
                                 :id :layer-visibility
                                 :child [:div "XXXXXXX"]}
                                {:title "Map Color"
                                 :id :map-color
                                 :child [:div "yyyyyyyy"]}
                                {:title "Polygon opacity"
                                 :id :polygon-opacity
                                 :child [:div "aaaaaaaaaaa"]}]}]
    [collapsible-tabs {:title "Filters"
                       :id :filters
                       :childs [{:title "States prob"
                                 :id :states-prob
                                 :child [:div "BALBALBALB"]}
                                {:title "Node Name"
                                 :id :node-name
                                 :child [:div "UUUUUUUUU"]}]}]]
   [:div.export-panel
    [:button.export {:on-click #(dispatch [:map/download-current-as-svg time])}
     "Export results"]]])

(defn main-screen []
  (let [time-ref (reagent/atom 0)]
   (fn []
     [:div.main-screen
      [top-bar]
      [controls-side-bar @time-ref]
      [animated-data-map time-ref]])))
