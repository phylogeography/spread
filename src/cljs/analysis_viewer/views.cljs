(ns analysis-viewer.views
  "Render maps and analysis data as hiccup svg vectors.
  Also handles animations."
  (:require [analysis-viewer.components :refer [switch-button slider]]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.svg-renderer :as svg-renderer]
            [clojure.string :as str]
            [goog.string :as gstr]
            [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [shared.math-utils :as math-utils]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)


(defn svg-node-object [{:keys [coord show-start show-end id]} _ time-perc params]
  (let [{:keys [nodes? nodes-radius nodes-color]} params
        show? (and (<= show-start time-perc show-end)
                   nodes?)
        [x1 y1] coord]
    
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:id id
               :cx x1
               :cy y1
               :r (* nodes-radius 0.5)
               :opacity 0.2
               :fill nodes-color}]]))

(defn svg-circle-object [{:keys [coord show-start show-end count-attr id]} _ time-perc params]
  (let [{:keys [circles? circles-radius circles-color]} params
        show? (and (<= show-start time-perc show-end)
                   circles?)
        [x1 y1] coord]
    
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:id id
               :cx x1
               :cy y1
               :r (* circles-radius count-attr)
               :opacity 0.2
               :fill circles-color}]]))

(defn svg-polygon-object [{:keys [coords show-start show-end id]} _ time-perc params]
  (let [{:keys [polygons? polygons-color polygons-opacity]} params
        show? (and (<= show-start time-perc show-end)
                   polygons?)]
    
    [:g {:style {:display (if show? :block :none)}}
     [:polygon
      {:id id
       :points (->> coords
                    (map (fn [coord] (str/join " " coord)))
                    (str/join ","))
       
       :fill polygons-color
       :opacity polygons-opacity}]]))

(defn svg-transition-object [{:keys [from-coord to-coord show-start show-end id attr-color]} _ time-perc params]
  (let [{:keys [transitions? transitions-color transitions-width transitions-curvature missiles?]} params
        show? (and (<= show-start time-perc show-end)
                   transitions?)
        clip-perc (when show?
                    (/ (- time-perc show-start)
                       (- show-end show-start)))        
        [x1 y1] from-coord
        [x2 y2] to-coord
        {:keys [f1]} (math-utils/quad-curve-focuses x1 y1 x2 y2 transitions-curvature)
        [f1x f1y] f1
        effective-color (or attr-color transitions-color) ;; attribute color takes precedence over transitions color
        curve-path-info {:id id
                         :d (str "M " x1 " " y1 " Q " f1x " " f1y " " x2 " " y2)
                         :stroke effective-color
                         :stroke-width transitions-width
                         :fill :transparent}
        missile-size 0.3]
    
    [:g {:style {:display (if show? :block :none)}}
     [:circle {:cx x1 :cy y1 :r 0.05 :fill effective-color}] 
     [:circle {:cx x2 :cy y2 :r 0.05 :fill effective-color}]
     [:path (if clip-perc

              ;; animated dashed curves
              (let [c-length (math-utils/quad-curve-length x1 y1 f1x f1y x2 y2)]
                (assoc curve-path-info
                       :stroke-dasharray (if missiles?
                                           [(* missile-size c-length) (* (- 1 missile-size) c-length)]
                                           c-length) 
                       :stroke-dashoffset (- c-length (* c-length clip-perc))))
 
              ;; normal curves
              curve-path-info)]]))

(defn map-primitive-object [{:keys [type] :as primitive-object} scale time params]
  (case type
    :transition (svg-transition-object primitive-object scale time params)
    :node (svg-node-object primitive-object scale time params)
    :circle (svg-circle-object primitive-object scale time params)
    :polygon (svg-polygon-object primitive-object scale time params)))

(def left-button  0)
(def wheel-button 1)
(def right-button 2)

(def animation-delta-t 50)
(def animation-increment 0.02)



(defn animation-controls []
  (let [time @(subscribe [:animation/percentage])
        playing? (= :play @(subscribe [:animation/state]))
        ticks-data @(subscribe [:analysis/data-timeline])
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
       
       (binding [svg-renderer/*coord-transform-fn* math-utils/map-coord->proj-coord]
         (svg-renderer/geojson->svg geo-json-map
                                   map-options))])))

(defn data-group []
  (let [time @(re-frame/subscribe [:animation/percentage])
        analysis-data  (vals @(re-frame/subscribe [:analysis/colored-data]))
        {:keys [scale]} @(re-frame/subscribe [:map/state])
        params (merge @(subscribe [:ui/parameters])
                      @(subscribe [:switch-buttons/states]))]
    (when analysis-data
      [:g {}
       ;; for debugging the data view-box
       #_(let [{:keys [x1 y1 x2 y2]} (events.maps/get-analysis-objects-view-box analysis-data)]
         [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke :red :stroke-width 0.1 :fill :transparent}])
       
       (for [primitive-object analysis-data]
         ^{:key (str (:id primitive-object))}
         [map-primitive-object primitive-object scale time params])])))

(defn object-attributes-popup [selected-obj]
  (let [[x y] @(re-frame/subscribe [:map/popup-coord])]
    [:div.pop-up.selected-object-popup
     {:style {:left x
              :top y}}
     (when selected-obj
       (for [[alabel aval] (:attrs selected-obj)]
         ^{:key (str alabel)}
         [:div.attr
          [:label (name alabel)]
          [:span (str aval)]]))]))

(defn possible-objects-selector-popup [possible-objects]
  (let [[x y] @(re-frame/subscribe [:map/popup-coord])]
    [:div.pop-up.possible-objects-popup
     {:style {:left x
              :top y}}
     (when possible-objects       
       (for [po possible-objects]
         ^{:key (:id po)}
         [:div.selectable-object {:on-click #(dispatch [:map/show-object-attributes (:id po) [x y]])}
          (str (:id po))]))]))

(defn data-map []
  (let [update-after-render (fn [div-cmp]                             
                              (let [dom-node (rdom/dom-node div-cmp)                                   
                                    brect (.getBoundingClientRect dom-node)]                                
                                (dispatch [:map/set-dimensions {:width (.-width brect)
                                                                :height (.-height brect)}])))
        last-mouse-down-coord (reagent/atom nil)]
   (reagent/create-class
    {:component-did-mount (fn [this] (update-after-render this))
     ;; :component-did-update (fn [this] (update-after-render this))
     :reagent-render
     (fn []
       (let [{:keys [grab translate scale zoom-rectangle]} @(re-frame/subscribe [:map/state])
             show-map? @(re-frame/subscribe [:switch-buttons/on? :show-map?])
             scale (or scale 1)
             [translate-x translate-y] translate]
         [:div.map-wrapper {:on-wheel (fn [evt]
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
                                                 (dispatch [:map/zoom-rectangle-grab {:x x :y y}]))
                                               (reset! last-mouse-down-coord [x y])))
                            
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
                                           
                                           (let [x (-> evt .-nativeEvent .-offsetX)
                                                 y (-> evt .-nativeEvent .-offsetY)]
                                             (cond

                                               ;; left button pressed
                                               (= left-button (.-button evt))                                               
                                               (do
                                                 (dispatch [:map/hide-object-attributes])
                                                 (dispatch [:map/hide-object-selector])
                                                 (when (= [x y] @last-mouse-down-coord)

                                                   ;; mouse was released without being moved, a click in place
                                                   ;; so check if we need to show any attributes
                                                   (let [elements-under-mouse (js/document.elementsFromPoint (-> evt .-nativeEvent .-clientX) (-> evt .-nativeEvent .-clientY))
                                                         offset-coord [(-> evt .-nativeEvent .-offsetX) (-> evt .-nativeEvent .-offsetY)]
                                                         ids (keep (fn [e]
                                                                     (let [eid (.-id e)]
                                                                       (when (and eid (str/starts-with? eid "object-"))
                                                                         eid)))
                                                                   elements-under-mouse)]
                                                     (cond
                                                       (= (count ids) 1)                                                     
                                                       (dispatch [:map/show-object-attributes (first ids) offset-coord])
                                                       
                                                       (> (count ids) 1)                                                     
                                                       (dispatch [:map/show-object-selector ids offset-coord]))))

                                                 (when grab
                                                   (dispatch [:map/grab-release])))
                                               
                                               
                                               ;; wheel button pressed
                                               (= wheel-button (.-button evt))
                                               (dispatch [:map/zoom-rectangle-release]))))}                   

          (when-let [selected-obj @(re-frame/subscribe [:analysis/selected-object])]
            [object-attributes-popup selected-obj])
          
          (when-let [possible-objects @(re-frame/subscribe [:analysis/possible-objects])]
            [possible-objects-selector-popup possible-objects])
          
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
            [:svg {:view-box "0 0 360 180" :preserve-aspect-ratio "xMinYMin"}
             (when show-map? [map-group])
             [data-group]]]

           (when zoom-rectangle
             (let [[x1 y1] (:origin zoom-rectangle)
                   [x2 y2] (:current zoom-rectangle)]
               [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke "#DD0808" :fill :transparent}]))]]))})))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Continuous tree settings ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn continuous-tree-layers-settings []
  [:div.layers-settings
   [:label "Map"]         [switch-button {:id :show-map?}]
   [:label "Map Borders"] [switch-button {:id :map-borders?}]
   [:label "Map Labels"]  [switch-button {:id :map-labels?}]
   [:label "Transitions"] [switch-button {:id :transitions?}]
   [:label "Polygons"]    [switch-button {:id :polygons?}]
   [:label "Nodes"]       [switch-button {:id :nodes?}]])

(defn color-chooser [{:keys [param-name]}]
  (let [colors ["#079DAB" "#3428CA" "#757295" "#DD0808" "#EEBE53" "#B20707" "#ECEFF8" "#FBFCFE" "#DEDEE9"
                "#266C08" "#C76503" "#1C58D0" "#A5387B" "#1C58D9" "#3A3668" "#757299" "#DEDEE8" "#3428CB"]
        selected-color @(subscribe [:parameters/selected param-name])]
    [:div.colors
     (for [c colors]
       ^{:key c}
       [:div.color {:style {:background-color c}
                    :on-click (fn [evt]
                                (.stopPropagation evt)
                                (dispatch [:parameters/select param-name c]))}
        (when (= selected-color c)
          [:i.zmdi.zmdi-check])])]))

(defn attribute-color-chooser [key]
  (let [attributes @(subscribe [:analysis/linear-attributes])
        [attr-key attr-range color-range] @(subscribe [:ui/parameters key])
        [color-from color-to] (or color-range ["#ffffff" "#ffffff"])]
    [:div.attribute-color-chooser
     [:select.attribute {:on-change #(let [sel-attr-key (-> % .-target .-value)]
                                       (dispatch [:parameters/select key [sel-attr-key
                                                                          (get attributes sel-attr-key)
                                                                          [color-from color-to]]]))
                         :value (or attr-key (ffirst attributes))}
      (for [a (keys attributes)]
        ^{:key (str a)}
        [:option {:value a} a])]
     [:input (cond-> {:type :color
                      :on-change #(dispatch [:parameters/select key [attr-key
                                                                     attr-range
                                                                     [(-> % .-target .-value) color-to]]])}
               color-from (assoc :value color-from))]
     [:input (cond-> {:type :color
                      :on-change #(dispatch [:parameters/select key [attr-key
                                                                     attr-range
                                                                     [color-from (-> % .-target .-value)]]])}
               color-to (assoc :value color-to))]]))

(defn continuous-tree-map-settings []
  [:div.map-settings
   [color-chooser {:param-name :map-borders-color}]])

(defn continuous-transitions-settings []
  [:div.transitions-settings
   [:div
    [:label.missile "Missiles"] [switch-button {:id :missiles?}]]
   
   [color-chooser {:param-name :transitions-color}]

   [attribute-color-chooser :transitions-attribute]

   [:label "Curvature"]
   [slider {:inc-buttons 0.1
            :min-val 0.1
            :max-val 2
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :transitions-curvature]
            :ev-vec [:parameters/select :transitions-curvature]}]

   [:label "Width"]
   [slider {:inc-buttons 0.05
            :min-val 0
            :max-val 0.3
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :transitions-width]
            :ev-vec [:parameters/select :transitions-width]}]])

(defn continuous-polygons-settings []
  [:div.polygons-settings
   [color-chooser {:param-name :polygons-color}]
   [:label "Opacity"]
   [slider {:inc-buttons 0.1
            :min-val 0
            :max-val 1
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :polygons-opacity]
            :ev-vec [:parameters/select :polygons-opacity]}]])

(defn continuous-tree-side-bar []
  [:div.tabs
   [collapsible-tabs {:title "Settings"
                      :id :parameters
                      :childs [{:title "Layers"
                                :id :continuous-tree-layer-settings
                                :child [continuous-tree-layers-settings]}
                               {:title "Map"
                                :id :continuous-tree-map-settings
                                :child [continuous-tree-map-settings]}
                               {:title "Transitions"
                                :id :transitions
                                :child [continuous-transitions-settings]}
                               {:title "Polygons"
                                :id :polygons
                                :child [continuous-polygons-settings]}]}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs []}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discrete tree settings ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn discrete-tree-layers-settings []
  [:div.layers-settings
   [:label "Map"]         [switch-button {:id :show-map?}]
   [:label "Map Borders"] [switch-button {:id :map-borders?}]
   [:label "Map Labels"]  [switch-button {:id :map-labels?}]
   [:label "Transitions"] [switch-button {:id :transitions?}]
   [:label "Circles"]     [switch-button {:id :circles?}]
   [:label "Nodes"]       [switch-button {:id :nodes?}]
   [:label "Labels"]      [switch-button {:id :labels?}]])

(def discrete-tree-map-settings continuous-tree-map-settings)
(def discrete-transitions-settings continuous-transitions-settings)

(defn discrete-circles-settings []
  [:div.circles-settings
   [color-chooser {:param-name :circles-color}]
   [attribute-color-chooser :circles-attribute]
   [:label "Size"]
   [slider {:inc-buttons 0.1
            :min-val 0
            :max-val 1
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :circles-radius]
            :ev-vec [:parameters/select :circles-radius]}]])

(defn discrete-nodes-settings []
  [:div.nodes-settings
   [color-chooser {:param-name :nodes-color}]
   [attribute-color-chooser :nodes-attribute]
   [:label "Size"]
   [slider {:inc-buttons 0.1
            :min-val 0
            :max-val 1
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :nodes-size]
            :ev-vec [:parameters/select :nodes-size]}]])

(defn discrete-labels-settings []
  [:div.labels-settings
   [color-chooser {:param-name :labels-color}]
   [:label "Size"]
   [slider {:inc-buttons 0.1
            :min-val 0
            :max-val 2
            :length 140
            :vertical? false
            :subs-vec [:ui/parameters :labels-size]
            :ev-vec [:parameters/select :labels-size]}]])

(defn discrete-tree-side-bar []
  [:div.tabs
   [collapsible-tabs {:title "Settings"
                      :id :parameters
                      :childs [{:title "Layers"
                                :id :discrete-tree-layer-settings
                                :child [discrete-tree-layers-settings]}
                               {:title "Map"
                                :id :discrete-tree-map-settings
                                :child [discrete-tree-map-settings]}
                               {:title "Transitions"
                                :id :transitions
                                :child [discrete-transitions-settings]}
                               {:title "Circles"
                                :id :circles
                                :child [discrete-circles-settings]}
                               {:title "Nodes"
                                :id :nodes
                                :child [discrete-nodes-settings]}
                               {:title "Labels"
                                :id :labels
                                :child [discrete-labels-settings]}]}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs []}]])

(defn bayes-factor-side-bar []
  [:div.tabs
   [collapsible-tabs {:title "Settings"
                      :id :parameters
                      :childs []}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs []}]])

(defn controls-side-bar [analysis-type]
  (if-not analysis-type
    [:div "Loading..."]
    [:div.side-bar
     (case analysis-type
       :ContinuousTree [continuous-tree-side-bar]
       :DiscreteTree   [discrete-tree-side-bar]
       :BayesFactor    [bayes-factor-side-bar])
     [:div.export-panel
      [:button.export {:on-click #(dispatch [:map/download-current-as-svg])}
       "Export results"]]]))

(defn main-screen []
  (let [analysis-type @(re-frame/subscribe [:analysis.data/type])]
    [:div.main-screen
     [top-bar]     
     [controls-side-bar analysis-type]
     [:div.animated-data-map      
      [data-map]
      [animation-controls]]]))
