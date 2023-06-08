(ns analysis-viewer.views
  "Render maps and analysis data as hiccup svg vectors.
  Also handles animations."
  (:require [analysis-viewer.components :refer [slider switch-button]]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.subs :as viewer-subs]
            [analysis-viewer.svg-renderer :as svg-renderer]
            [clojure.string :as str]
            [goog.string :as gstr]
            [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [shared.components :refer [collapsible-tab mui-slider spread-logo]]
            [shared.math-utils :as math-utils]
            [ui.utils :as ui-utils]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ATTENTION!! This functions (svg-transition-object, svg-node-object, etc)                               ;;
;; should never be converted to reagent components, since they are also used by the svg renderer system   ;;
;; which doesn't understand components                                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn svg-node-object [{:keys [coord id]} params {:keys [visible?]}]
  (let [{:keys [nodes-radius]} params
        [x1 y1] coord]

    [:g {:id id
         :style {:display (if visible? "block" "none")
                 :cursor :cell}
         :class "data-node"}
     [:circle {:id (str id "-touchable")
               :cx x1
               :cy y1
               :r (* nodes-radius 0.5)
               :opacity 0.2}]]))

(defn svg-circle-object [{:keys [coord count-attr id]} params {:keys [visible?]}]
  (let [{:keys [circles-radius]} params
        [x1 y1] coord]

    [:g {:id id
         :style {:display (if visible? "block" "none")
                 :cursor :cell}
         :class "data-circle"}
     [:circle {:id (str id "-touchable")
               :cx x1
               :cy y1
               :r (* circles-radius count-attr)
               :opacity 0.2}]]))

(defn svg-polygon-object [{:keys [coords id]} params {:keys [visible?]}]
  (let [{:keys [polygons-opacity]} params]

    [:g {:id id
         :style {:display (if visible? "block" "none")
                 :cursor :cell}
         :class "data-polygon"}
     [:polygon
      {:id (str id "-touchable")
       :stroke-width 0.05
       :points (->> coords
                    (mapv (fn [coord] (str/join " " coord)))
                    (str/join ","))
       :opacity polygons-opacity}]]))

(defn svg-transition-object [{:keys [from-coord to-coord id] :as transition} curr-timestamp params {:keys [visible?]}]
  (let [{:keys [transitions-width missiles?]} params
        [x1 y1] from-coord
        [x2 y2] to-coord
        {:keys [f1x f1y c-length] :as transition} (viewer-subs/add-obj-presentation-attrs transition params)
        {:keys [stroke-dashoffset]} (viewer-subs/calc-obj-time-attrs transition curr-timestamp params)
        missile-size 0.3]

    [:g {:id id
         :style {:display (if visible? "block" "none")
                 :cursor :cell}
         :class "data-transition"}
     ;; Let's comment out source and destination of a transition since they
     ;; don't make much sense. Maybe we can use them for debugging or something.
     #_[:circle {:class "data-transition-from" :cx x1 :cy y1 :r 0.05 :fill :black}]
     #_[:circle {:class "data-transition-to" :cx x2 :cy y2 :r 0.05 :fill :black}]
     #_[:text {:x x1 :y y1 :fill :black :font-size 0.2} (:from-point-id transition)]
     #_[:text {:x x2 :y y2 :fill :black :font-size 0.2} (:to-point-id transition)]
     [:path {:class "data-transition-path"
             :id (str id "-touchable")
             :d (str "M " x1 " " y1 " Q " f1x " " f1y " " x2 " " y2)
             :stroke-width transitions-width
             :fill :transparent
             :stroke-dasharray (if missiles?
                                 [(* missile-size c-length) (* (- 1 missile-size) c-length)]
                                 c-length)
             :data-curve-length c-length
             :stroke-dashoffset (if (nil? curr-timestamp) 0 stroke-dashoffset)}]]))


(defn map-primitive-object [{:keys [type] :as primitive-object} curr-timestamp params opts]
  (case type
    :transition (svg-transition-object primitive-object curr-timestamp params opts)
    :node (svg-node-object primitive-object params opts)
    :circle (svg-circle-object primitive-object params opts)
    :polygon (svg-polygon-object primitive-object params opts)))

(defn text-group [data-objects params {:keys [visible?]}]
  (let [text-color (if (get params :labels?)
                     (get params :labels-color "#079DAB")
                     :transparent)
        font-size (str (:labels-size params) "px")]
    [:g {}
     (for [obj data-objects]

       [:g {:id (str (:id obj) "-text")
            :key (:id obj)
            :style {:display (if visible? "block" "none")}}
        (if (= :transition (:type obj))
          ;; it is a transition add labels for src and dst
          (let [[x1 y1] (:from-coord obj)
                [x2 y2] (:to-coord obj)]
            [:g {}
             [:text {:x x1 :y y1
                     :class "data-text"
                     :font-size font-size
                     :fill text-color
                     :text-anchor "middle"}
              (:from-label obj)]
             [:text {:x x2 :y y2
                     :class "data-text"
                     :font-size font-size
                     :fill text-color
                     :text-anchor "middle"}
              (:to-label obj)]])

          ;; other kinds of objects only contains one label
          (let [[x y] (:coord obj)]
            [:text {:x x :y y
                    :font-size font-size
                    :fill text-color
                    :class "data-text"
                    :text-anchor "middle"}
             (:label obj)]))])]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reagent components start here ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def left-button  0)
(def wheel-button 1)
(def right-button 2)

(def animation-delta-t 50)
(def animation-increment 0.02)

(defn timeline []
  (let [crop-sides 20
        ticks-y-base 80
        timeline-start crop-sides
        update-after-render (fn [div-cmp]
                              (let [dom-node (rdom/dom-node div-cmp)
                                    brect (.getBoundingClientRect dom-node)]
                                (dispatch [:analysis.timeline/set-width (.-width brect)])))]
    (reagent/create-class
      {:component-did-mount (fn [this] (update-after-render this))
       :reagent-render
       (fn []
         (let [frame-timestamp @(subscribe [:animation/frame-timestamp])
               ticks-data @(subscribe [:analysis/data-timeline])
               [date-from-millis date-to-millis] @(subscribe [:analysis/date-range])
               full-length-px (apply max (map :x ticks-data))
               date-range->px-rescale (math-utils/build-scaler date-from-millis date-to-millis 0 full-length-px)
               [crop-low-millis crop-high-millis] @(subscribe [:animation/crop])
               crop-left (date-range->px-rescale crop-low-millis)
               crop-width (+ (- (date-range->px-rescale crop-high-millis) (date-range->px-rescale crop-low-millis))
                             (* 2 timeline-start)
                             (- 7))
               frame-line-x (+ timeline-start (date-range->px-rescale frame-timestamp))]
           [:div.timeline {:width "100%" :height "100%"}
            (when ticks-data
              [:<>
               [:div.crop-box {:style {:left crop-left
                                       :width crop-width}}
                [:i.left.zmdi.zmdi-chevron-left   {:style {:width (str crop-sides "px")}}]
                [:i.right.zmdi.zmdi-chevron-right {:style {:width (str crop-sides "px")}}]]
               [:svg {:width "100%" :height "110px"}
                [:g
                 [:line {:x1 frame-line-x :y1 0
                         :x2 frame-line-x :y2 120
                         :stroke "#EEBE53"
                         :stroke-width 2}]
                 (for [{:keys [label x type]} ticks-data]
                   (let [x (+ x timeline-start)]
                     ^{:key (str x)}
                     [:g
                      [:line {:x1 x :y1 ticks-y-base
                              :x2 x :y2 (- ticks-y-base (if (= type :short) 5 10))
                              :stroke "#3A3668"}]

                      ;; NOTE: Enable for debugging
                      #_(when (pos? perc)
                          [:line {:x1 x :y1 ticks-bars-y-base
                                  :x2 x :y2 (- ticks-bars-y-base (/ (* perc ticks-bars-full) 100))
                                  :stroke "red"}])

                      (when label
                        [:text {:x x :y (+ ticks-y-base 10) :font-size 10 :fill "#3A3668" :stroke :transparent :text-anchor :middle}
                         label])]))]]])]))})))

(defn animation-controls []
  (let [frame-timestamp @(subscribe [:animation/frame-timestamp])
        desired-duration @(subscribe [:animation/desired-duration])
        playing? (= :play @(subscribe [:animation/state]))]

    ;; IMPORTANT ! For some reason in chrome when the animation-controls get updated (when :animation/frame-timestamp) changes for example,
    ;; the entire map is being redraw, making some animations a lot less smooth.
    ;; {:contain :strict} prevents just that, tells the browser that changes inside this element shouldn't affect elements outside.
    [:div.animation-controls {:style {:contain :strict}}
     [mui-slider {:inc-buttons 1000
                  :class "desired-duration-slider"
                  :min-val 1
                  :max-val 300000 ;; 5 minutes max
                  :vertical? true
                  :subs-vec [:animation/desired-duration]
                  :ev-vec [:animation/set-desired-duration]}]
     (if (zero? frame-timestamp)
       [:div.loading "Loading..."]
       [:div.inner
        [:div.hud
         [:div.speed (gstr/format "Total animation duration: %d sec" (/ desired-duration 1000))]
         [:div.date (str "Date: " (ui-utils/format-date frame-timestamp))]]
        [:div.buttons
         [:i.zmdi.zmdi-skip-previous {:on-click #(dispatch [:animation/reset :start])} ""]
         [:i.zmdi.zmdi-caret-left {:on-click #(dispatch [:animation/prev])} ""]
         [:i.zmdi {:on-click #(dispatch [:animation/toggle-play-stop])
                   :class (if playing? "zmdi-pause" "zmdi-play")}]
         [:i.zmdi.zmdi-caret-right {:on-click #(dispatch [:animation/next])} ""]
         [:i.zmdi.zmdi-skip-next {:on-click #(dispatch [:animation/reset :end])} ""]]
        [timeline]])]))

(defn map-group []
  (let [map-options @(re-frame/subscribe [:map/parameters])]
    (fn []
      (let [geo-json-map @(re-frame/subscribe [:map/data])]
        (when geo-json-map
          [:g {}
           (binding [svg-renderer/*coord-transform-fn* math-utils/map-coord->proj-coord]
             (svg-renderer/geojson->svg geo-json-map
                                        map-options))])))))

(defn data-group []
  (let [params (merge @(subscribe [:ui/parameters])
                      @(subscribe [:switch-buttons/states]))]
    (fn []
      (let [analysis-data  @(re-frame/subscribe [:analysis/filtered-data-sorted])
            analysis-type @(re-frame/subscribe [:analysis.data/type])
            ;; we react/redraw only to this param changes
            circle-radius @(subscribe [:ui/parameters :circles-radius])
            transition-curvature @(subscribe [:ui/parameters :transitions-curvature])
            missiles? @(subscribe [:switch-buttons/on? :missiles?])
            params (assoc params
                          :circles-radius circle-radius
                          :transitions-curvature transition-curvature
                          :missiles? missiles?)]
        (when analysis-data
          [:g {}
           ;; for debugging the data view-box
           #_(let [{:keys [x1 y1 x2 y2]} (events.maps/get-analysis-objects-view-box analysis-data)]
               [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke :red :stroke-width 0.1 :fill :transparent}])

           (for [primitive-object analysis-data]
             ^{:key (str (:id primitive-object))}
             [map-primitive-object
              primitive-object
              (if (= analysis-type :BayesFactor) nil 0) ;; - time is always zero in the animation path since it is handled by the animation subsystem params, bayes factor analysis doesn't have time
              params
              {:visible? (= analysis-type :BayesFactor)} ;; - this is the animation path so we start with all data invisible unless it is the BayesFactor
              ])])))))                                   ;; analysis in which doesn't have animation


(defn text-group-component []
  (let [ui-params @(subscribe [:ui/parameters])
        switch-buttons @(subscribe [:switch-buttons/states])]
    (fn []
      (let [data-objects (vals @(re-frame/subscribe [:analysis/filtered-data]))]
        [text-group
         data-objects
         (merge ui-params switch-buttons)
         {:visible? false}] ;; this is the animation path so we start with all data invisible
        ))))

(defn object-attributes-popup [selected-obj]
  (let [[x y] @(re-frame/subscribe [:map/popup-coord])]
    [:div.pop-up.selected-object-popup
     {:on-wheel (fn [evt] (.stopPropagation evt))
      :on-mouse-down (fn [evt] (.stopPropagation evt))
      :on-mouse-move (fn [evt] (.stopPropagation evt))
      :on-mouse-up (fn [evt] (.stopPropagation evt))
      :style {:left x
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
         [:div.selectable-object {:on-click #(dispatch [:map/show-object-attributes (:id po) [x y]])
                                  :on-mouse-enter #(dispatch [:map/highlight-object (:id po)])
                                  :on-mouse-leave #(dispatch [:map/highlight-object nil])}
          (str (:id po))]))]))


(defn data-map []
  (let [update-after-render (fn [div-cmp]
                              (let [dom-node (rdom/dom-node div-cmp)
                                    brect (.getBoundingClientRect dom-node)]
                                (dispatch [:map/set-dimensions {:width (.-width brect)
                                                                :height (.-height brect)}])))
        last-mouse-down-coord (reagent/atom nil)
        on-data-map-wheel (fn [evt]
                            (let [x (-> evt .-nativeEvent .-offsetX)
                                  y (-> evt .-nativeEvent .-offsetY)]
                              (dispatch [:map/zoom-inc {:delta (.-deltaY evt)
                                                        :x x
                                                        :y y}])))
        on-data-map-mouse-down (fn [evt]
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
        on-context-menu (fn [evt]
                          (.preventDefault evt))

        on-mouse-up (fn [grab evt]
                      (.stopPropagation evt)
                      (.preventDefault evt)

                      (let [x (-> evt .-nativeEvent .-offsetX)
                            y (-> evt .-nativeEvent .-offsetY)]
                        (dispatch [:map/hide-object-attributes])
                        (dispatch [:map/hide-object-selector])
                        (cond

                          ;; left button pressed
                          (= left-button (.-button evt))
                          (do
                            (when (= [x y] @last-mouse-down-coord)

                              ;; mouse was released without being moved, a click in place
                              ;; so check if we need to show any attributes
                              (let [elements-under-mouse (js/document.elementsFromPoint (-> evt .-nativeEvent .-clientX) (-> evt .-nativeEvent .-clientY))
                                    offset-coord [(-> evt .-nativeEvent .-offsetX) (-> evt .-nativeEvent .-offsetY)]
                                    ids (keep (fn [e]
                                                (let [eid (.-id e)]
                                                  (when (and eid (str/starts-with? eid "object-"))
                                                    (second (re-find #"(object-[\d]+)\-touchable" eid)))))
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
                          (dispatch [:map/zoom-rectangle-release]))))
        on-mouse-move (fn [grab zoom-rectangle evt]
                        (let [x (-> evt .-nativeEvent .-offsetX)
                              y (-> evt .-nativeEvent .-offsetY)]
                          (cond
                            grab
                            (dispatch [:map/drag {:x x :y y}])

                            zoom-rectangle
                            (dispatch [:map/zoom-rectangle-update {:x x :y y}]))))
        ]
   (reagent/create-class
    {:component-did-mount (fn [this] (update-after-render this))
     ;; :component-did-update (fn [this] (update-after-render this))
     :reagent-render
     (fn []
       (let [{:keys [grab translate scale zoom-rectangle]} @(re-frame/subscribe [:map/state])
             show-map? @(re-frame/subscribe [:switch-buttons/on? :show-map?])
             scale (or scale 1)
             [translate-x translate-y] translate
             possible-objects @(re-frame/subscribe [:analysis/possible-objects])]
         [:div.map-wrapper {:on-wheel on-data-map-wheel
                            :on-mouse-down on-data-map-mouse-down
                            :on-mouse-move (partial on-mouse-move grab zoom-rectangle)
                            :on-mouse-up (partial on-mouse-up grab)
                            :on-context-menu on-context-menu}
          (when-let [selected-obj @(re-frame/subscribe [:analysis/selected-object])]
            [object-attributes-popup selected-obj])

          (when possible-objects
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

           ;; map background
           [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill "#ECEFF8"}]

           ;; map and data svg
           [:g {:transform (gstr/format "translate(%f %f) scale(%f %f)"
                                        (or translate-x 0)
                                        (or translate-y 0)
                                        scale scale)}
            [:svg {:view-box "0 0 360 180" :preserve-aspect-ratio "xMinYMin"}
             (when show-map? [map-group])
             [data-group]
             [text-group-component]]]

           (when zoom-rectangle
             (let [[x1 y1] (:origin zoom-rectangle)
                   [x2 y2] (:current zoom-rectangle)]
               [:rect {:x x1 :y y1 :width (- x2 x1) :height (- y2 y1) :stroke "#DD0808" :fill :transparent}]))]]))})))

(defn top-bar []
  [:div.top-bar
   [spread-logo]])

(defn collapsible-tabs [{:keys [title childs]}]
  [:div.collapsible-tabs
   [:div.title title]
   [:div.tabs
    (for [c childs]
      ^{:key (str (:id c))}
      [collapsible-tab c])]])

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

(defn transitions-settings [{:keys [missiles-switch?]}]
  [:div.transitions-settings
   (when missiles-switch?
     [:div
      [:label.missile "Missiles"] [switch-button {:id :missiles?}]])

   [color-chooser {:param-name :transitions-color}]

   [attribute-color-chooser :transitions-attribute]

   [:label "Curvature"]
   [mui-slider {:inc-buttons 0.1
                :min-val 0.1
                :max-val 2
                :vertical? false
                :subs-vec [:ui/parameters :transitions-curvature]
                :ev-vec [:parameters/select :transitions-curvature]}]

   [:label "Width"]
   [mui-slider {:inc-buttons 0.05
                :min-val 0
                :max-val 0.3
                :vertical? false
                :subs-vec [:ui/parameters :transitions-width]
                :ev-vec [:parameters/select :transitions-width]}]])

(defn continuous-polygons-settings []
  [:div.polygons-settings
   [color-chooser {:param-name :polygons-color}]
   [:label "Opacity"]
   [mui-slider {:inc-buttons 0.1
                :min-val 0
                :max-val 1
                :vertical? false
                :subs-vec [:ui/parameters :polygons-opacity]
                :ev-vec [:parameters/select :polygons-opacity]}]])

(defn continuous-animation-settings []
  (let [[from-millis to-millis] @(subscribe [:analysis/date-range])
        [crop-from-millis crop-to-millis] @(subscribe [:animation/crop])
        playing? (= :play @(subscribe [:animation/state]))
        df (js/Date. from-millis)
        dt (js/Date. to-millis)
        cf (js/Date. crop-from-millis)
        ct (js/Date. crop-to-millis)
        min-date-str      (gstr/format "%d-%02d-%02d" (.getUTCFullYear df) (inc (.getUTCMonth df)) (.getUTCDate df))
        max-date-str      (gstr/format "%d-%02d-%02d" (.getUTCFullYear dt) (inc (.getUTCMonth dt)) (.getUTCDate dt))
        crop-min-date-str (gstr/format "%d-%02d-%02d" (.getUTCFullYear cf) (inc (.getUTCMonth cf)) (.getUTCDate cf))
        crop-max-date-str (gstr/format "%d-%02d-%02d" (.getUTCFullYear ct) (inc (.getUTCMonth ct)) (.getUTCDate ct))
        set-new-crop (fn [crop]
                       (dispatch [:animation/set-crop crop]))]

    [:div.animation-settings
     [:label "From:"]
     [:input {:type :date
              :disabled playing?
              :on-change (fn [evt]
                           (set-new-crop [(js/Date.parse (.-value (.-target evt)))
                                          crop-to-millis]))
              :min min-date-str
              :max max-date-str
              :value crop-min-date-str}]
     [:label "To:"]
     [:input {:type :date
              :disabled playing?
              :on-change (fn [evt]
                           (set-new-crop [crop-from-millis
                                          (js/Date.parse (.-value (.-target evt)))]))
              :min min-date-str
              :max max-date-str
              :value crop-max-date-str}]]))

(defn ordinal-attribute-filter [f]
  (let [checked-set (:filter-set f)]
    [:div.ordinal-attribute-filter
     (for [item (:domain (:attribute f))]
       ^{:key item}
       [:div
        [:label
         [:input {:type :checkbox
                  :name item
                  :on-change (fn [evt]
                               (let [checked? (-> evt .-target .-checked)
                                     item (-> evt .-target .-name)]
                                 (if checked?
                                   (dispatch [:filters/add-ordinal-attribute-filter-item (:filter/id f) item])
                                   (dispatch [:filters/rm-ordinal-attribute-filter-item  (:filter/id f) item]))))
                  :checked (boolean (checked-set item))}]
         [:span.text item]]])]))

(defn linear-attribute-filter [f]
  [:div.linear-attribute-filter
   [mui-slider {:inc-buttons 0.1
                :min-val 0
                :max-val 1
                :vertical? false
                :subs-vec [:analysis.data/linear-attribute-filter-range (:filter/id f)]
                :ev-vec [:filters/set-linear-attribute-filter-range (:filter/id f)]}]])

(defn continuous-attributes-filters []
  (let [attributes (subscribe [:analysis/attributes])
        filters (subscribe [:analysis.data/attribute-filters])
        selected-attr (reagent/atom (first (keys @attributes)))]
    (fn []
      [:div.attribute-filters
      [:div.selection
       [:select.attribute {:on-change #(reset! selected-attr (-> % .-target .-value))
                           :value @selected-attr}
        (for [a (keys @attributes)]
          ^{:key (str a)}
          [:option {:value a} a])]
       [:i.zmdi.zmdi-plus-circle {:on-click #(dispatch [:filters/add-attribute-filter @selected-attr])}]]

      (for [f (vals @filters)]
        ^{:key (:filter/id f)}
        [:div.filter
         [:div.top-bar
          [:span (:attribute/id f)]
          [:i.zmdi.zmdi-close-circle {:on-click #(dispatch [:filters/rm-attribute-filter (:filter/id f)])}]]
         (case (:filter/type f)
           :linear-filter  [linear-attribute-filter f]
           :ordinal-filter [ordinal-attribute-filter f])])])))

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
                                :child [transitions-settings {:missiles-switch? true}]}
                               {:title "Polygons"
                                :id :polygons
                                :child [continuous-polygons-settings]}
                               {:title "Animation"
                                :id :animation-settings
                                :child [continuous-animation-settings]}]}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs [{:title "Attributes"
                                :id :attributes-range-filters
                                :child [continuous-attributes-filters]}]}]])

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
(def discrete-animation-settings continuous-animation-settings)
(def discrete-attributes-filters continuous-attributes-filters)

(defn discrete-circles-settings []
  [:div.circles-settings
   [color-chooser {:param-name :circles-color}]
   [attribute-color-chooser :circles-attribute]
   [:label "Size"]
   [mui-slider {:inc-buttons 0.1
                :min-val 0
                :max-val 1
                :vertical? false
                :subs-vec [:ui/parameters :circles-radius]
                :ev-vec [:parameters/select :circles-radius]}]])

(defn discrete-nodes-settings []
  [:div.nodes-settings
   [color-chooser {:param-name :nodes-color}]
   [attribute-color-chooser :nodes-attribute]
   [:label "Size"]
   [mui-slider {:inc-buttons 0.1
                :min-val 0
                :max-val 1
                :vertical? false
                :subs-vec [:ui/parameters :nodes-size]
                :ev-vec [:parameters/select :nodes-size]}]])

(defn discrete-labels-settings []
  [:div.labels-settings
   [color-chooser {:param-name :labels-color}]
   [:label "Size"]
   [mui-slider {:inc-buttons 0.1
                :min-val 0
                :max-val 2
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
                                :child [transitions-settings {:missiles-switch? true}]}
                               {:title "Circles"
                                :id :circles
                                :child [discrete-circles-settings]}
                               {:title "Nodes"
                                :id :nodes
                                :child [discrete-nodes-settings]}
                               {:title "Labels"
                                :id :labels
                                :child [discrete-labels-settings]}
                               {:title "Animation"
                                :id :animation-settings
                                :child [discrete-animation-settings]}]}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs [{:title "Attributes"
                                :id :attributes-range-filters
                                :child [discrete-attributes-filters]}]}]])

(defn bayes-tree-layers-settings []
  [:div.layers-settings
   [:label "Map"]         [switch-button {:id :show-map?}]
   [:label "Map Borders"] [switch-button {:id :map-borders?}]
   [:label "Map Labels"]  [switch-button {:id :map-labels?}]
   [:label "Transitions"] [switch-button {:id :transitions?}]
   [:label "Nodes"]       [switch-button {:id :nodes?}]
   [:label "Labels"]      [switch-button {:id :labels?}]])

(def bayes-tree-map-settings discrete-tree-map-settings)
(def bayes-attributes-filters continuous-attributes-filters)
(def bayes-nodes-settings discrete-nodes-settings)
(def bayes-labels-settings discrete-labels-settings)

(defn bayes-factor-side-bar []
  [:div.tabs
   [collapsible-tabs {:title "Settings"
                      :id :parameters
                      :childs [{:title "Layers"
                                :id :discrete-tree-layer-settings
                                :child [bayes-tree-layers-settings]}
                               {:title "Map"
                                :id :discrete-tree-map-settings
                                :child [bayes-tree-map-settings]}
                               {:title "Transitions"
                                :id :transitions
                                :child [transitions-settings {:missiles-switch? false}]}
                               {:title "Nodes"
                                :id :nodes
                                :child [bayes-nodes-settings]}
                               {:title "Labels"
                                :id :labels
                                :child [bayes-labels-settings]}]}]
   [collapsible-tabs {:title "Filters"
                      :id :filters
                      :childs [{:title "Attributes"
                                :id :attributes-range-filters
                                :child [bayes-attributes-filters]}]}]])

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

(defn params-styles []
  (let [params @(re-frame/subscribe [:ui/parameters])
        switch-buttons @(re-frame/subscribe [:switch-buttons/states])
        params-styles (viewer-subs/render-params-styles-string params switch-buttons)]
    [:style params-styles]))



(defn data-elements-styles []
  (let [objects-styles (viewer-subs/render-elements-styles-string @(re-frame/subscribe [:analysis/colored-and-filtered-data])
                                                      @(re-frame/subscribe [:analysis/highlighted-object-id]))]
    [:style objects-styles]))


(defn main-screen []
  (let [analysis-type @(re-frame/subscribe [:analysis.data/type])]
    [:div.main-screen
     [top-bar]
     [controls-side-bar analysis-type]
     [:div.animated-data-map
      [params-styles]
      [data-elements-styles]
      [data-map]
      (when (not= analysis-type :BayesFactor)
        [animation-controls])]]))
