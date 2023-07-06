(ns analysis-viewer.fxs
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [analysis-viewer.subs :as subs]
            [analysis-viewer.svg-renderer :as svg-renderer]
            [analysis-viewer.views :as views]
            [goog.string :as gstr]
            [hiccups.runtime :as hiccupsrt]
            [re-frame.core :as re-frame]
            [analysis-viewer.gl-utils :as gl-utils]
            [shared.math-utils :as math-utils]))

;;;;;;;;;;;;;;;;;;;;;;;
;; SVG File renderer ;;
;;;;;;;;;;;;;;;;;;;;;;;
(defn data-map [geo-json-map analysis-data analysis-data-box curr-timestamp params styles]
  (let [padding 10]
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

     [:style {:type "text/css"} styles]

     ;; map background
     [:rect {:x "0" :y "0" :width "360" :height "180" :fill (:background-color params)}]

     ;; map group
     [:g {}
      [:g {}
       (binding [svg-renderer/*coord-transform-fn* math-utils/map-coord->proj-coord]
         (svg-renderer/geojson->svg geo-json-map params))]

      ;; data group
      [:g {}
       (when analysis-data
         [:g {}
          (for [primitive-object analysis-data]
            ^{:key (str (:id primitive-object))}
            (views/map-primitive-object primitive-object curr-timestamp params {:visible? true}))])]

      ;; text group
      (views/text-group analysis-data params {:visible? true})]]))

(re-frame/reg-fx
 :spread/download-current-map-as-svg
 (fn [{:keys [geo-json-map styles analysis-data data-box curr-timestamp params]}]
   (let [time-filtered-data (filter (fn [obj] ;; don't include in file data not for this frame
                                      (:show? (subs/calc-obj-time-attrs obj curr-timestamp params)))
                                    analysis-data)
         svg-text (html (data-map geo-json-map time-filtered-data data-box curr-timestamp params styles))
         download-anchor (js/document.createElement "a")]
     (.setAttribute download-anchor "href" (str "data:image/svg+xml;charset=utf-8," (js/encodeURIComponent svg-text)))
     (.setAttribute download-anchor "download" "map.svg")
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor))))

;;;;;;;;;;;;;;;;
;; Animations ;;
;;;;;;;;;;;;;;;;

(defonce animation-running* (atom false))
(def report-freq-millis 1000)

(defn get-dom-elements [data-objects]
  (->> data-objects
       (reduce (fn [r {:keys [type] :as obj}]
                 (let [el      (js/document.getElementById (:id obj))
                       text-el (js/document.getElementById (str (:id obj) "-text"))]
                   (conj r (cond-> obj
                             true                 (assoc :group-elem el)
                             text-el              (assoc :text-elem text-el)
                             (= type :transition) (assoc :path-elem (.querySelector el ":scope > .data-transition-path"))))))
               [])))

(defn animation-repaint [elems curr-timestamp params]
  (doseq [{:keys [group-elem text-elem] :as obj} elems]

    (let [{:keys [show? in-change-range?] :as attrs} (subs/calc-obj-time-attrs obj curr-timestamp params)]

      ;; animate hide/show text elements
      (when text-elem
        (set! (-> text-elem .-style .-display ) (if show? "block" "none")))

      ;; animate hide/show elements
      (set! (-> group-elem .-style .-display ) (if show? "block" "none"))

      (when in-change-range?
        ;; animate transitions by moving the stroke-dashoffset
        (when (and show? (= :transition (:type obj)))
          (.setAttribute (:path-elem obj)
                         "stroke-dashoffset"
                         (str (:stroke-dashoffset attrs))))))))

(re-frame/reg-fx
  :animation/repaint
  (fn [{:keys [data-objects timestamp params]}]
    (let [elems (get-dom-elements data-objects)]
      (animation-repaint elems timestamp params))))

(re-frame/reg-fx
  :animation/start
  (fn [{:keys [data-objects desired-duration date-range params]}]
    (let [[date-from date-to] date-range
          date-range-millis (- date-to date-from)
          _ (println "TOTAL DAYS" (/ date-range-millis 1000 60 60 24))
          elems (get-dom-elements data-objects)
          start-time* (atom nil)
          last-report* (atom 0)
          step (fn step [t]
                 ;; `t` in millis, should be accurate to 5 µs in the fractional part of the double
                 (let [elapsed (if-let [st @start-time*]
                                 (- t st)
                                 (do
                                   (reset! start-time* t)
                                   1))

                       curr-frame-timestamp (+ date-from (* (/ elapsed (* 1000 desired-duration))
                                                            date-range-millis))]

                   ;; every `report-freq-millis` report to the re-frame db so the ui can keep track of
                   ;; animation progress
                   (when (> (- t @last-report*) report-freq-millis)
                     (re-frame/dispatch [:animation/update-frame-timestamp curr-frame-timestamp])
                     (reset! last-report* t))

                   ;; update all data objects dom nodes
                   (animation-repaint elems curr-frame-timestamp params)

                   ;; keep animating until we reach the end or someone
                   ;; stopps us
                   (cond
                     ;; animation finished
                     (>= curr-frame-timestamp date-to)
                     (do (re-frame/dispatch [:animation/finished])
                         (reset! animation-running* false))

                     @animation-running*
                     (js/requestAnimationFrame step))))]

      (reset! animation-running* true)
      (js/requestAnimationFrame step))))

(re-frame/reg-fx
 :animation/stop
 (fn [_]
   (reset! animation-running* false)))

;; NOTE: this is only for dev to support hot reloading while the animation is running,
;; nice for modifying animation code while animation keeps running.
;; It is ment to be called only from `analysis-viewer.main/mount-ui` which is called
;; on hot code reload, so if the animation was running we re run it again.
(defn maybe-re-run-animation []
  (when @animation-running*
    (re-frame/dispatch [:animation/toggle-play-stop])
    (js/setTimeout (fn []
                     (re-frame/dispatch [:animation/toggle-play-stop]))
                   500)))


(def entities-desc
  {:first {:vertices [ 0.0  1.0 0.0
                       1.0 -1.0 0.0
                      -1.0 -1.0 0.0]
           :attributes [{:name "aPos" :num-components 3}]
           :uniforms [{:name "ourColor" :type :vec4}]
           :vert-source "
attribute lowp vec3 aPos;

void main() {
  gl_Position = vec4(aPos, 1.0);
}"

           :frag-source "
uniform lowp vec4 ourColor;

void main() {
  gl_FragColor = ourColor;
}"}})

(def compiled-entities (atom {}))

(defn compile-entities [^js gl]
  (doseq [[shk {:keys [vertices vert-source frag-source attribues uniforms]}] entities-desc]
    (let [^js ent-program (gl-utils/link-shader-program gl vert-source frag-source)
          VBO (.createBuffer gl)]

      (.bindBuffer gl (.-ARRAY_BUFFER gl) VBO)
      (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. (clj->js vertices)) (.-STATIC_DRAW gl))

      (swap! compiled-entities assoc shk {:program ent-program
                                          :draw (fn [& uniforms-vals]
                                                  (let [attr-loc (.getAttribLocation gl ent-program "aPos") ]
                                                    (.bindBuffer gl (.-ARRAY_BUFFER gl) VBO)
                                                    (.vertexAttribPointer gl attr-loc 3 (.-FLOAT gl) false 0 0)
                                                    (.enableVertexAttribArray gl attr-loc)
                                                    (.useProgram gl ent-program)
                                                    (doseq [{:keys [name type]} uniforms]
                                                      (case type
                                                        :vec4 (let [[x y z w] uniforms-vals]
                                                                (.uniform4f gl (.getUniformLocation gl ent-program name) x y z w))))
                                                    (.drawArrays gl (.-TRIANGLE_STRIP gl) 0 (/ (count vertices) 3))))
                                          }))))

(defn gpu-init [_]
  (let [canvas (js/document.querySelector "#glcanvas")
        gl (.getContext canvas "webgl")]

    (compile-entities gl)

    ;; draw scene
    (let [draw-scene (fn draw-scene [t]
                       (doto gl
                         (.clearColor 0.5 0.0 0.0 1.0)
                         (.clearDepth 1.0)
                         (.enable (.-DEPTH_TEST gl))
                         (.depthFunc (.-LEQUAL gl))
                         (.clear (or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl))))
                       (doseq [[ent-key {:keys [draw]}] @compiled-entities]
                         (let [color (+ 0.5 (/ (js/Math.sin (* t 0.001)) 2))]
                           (draw color 0.2 0.3 1.0)))
                       (js/requestAnimationFrame draw-scene))]
      (js/requestAnimationFrame draw-scene)
      )
    ))

(re-frame/reg-fx
 :gpu/init
 gpu-init)
