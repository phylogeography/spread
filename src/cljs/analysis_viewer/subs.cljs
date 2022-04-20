(ns analysis-viewer.subs
  (:require [re-frame.core :refer [reg-sub]]
            [shared.math-utils :as math-utils]
            [shared.utils :as utils]))

(defn geo-json-data-map [db-maps]
  (let [maps {:type "FeatureCollection"
              :features (->> db-maps
                             (sort-by :map/z-index <)
                             (map :map/geo-json))}]
    maps))

(reg-sub
 :maps/all-maps-data
 (fn [db _]
  (:maps/data db)))

(reg-sub
 :map/state
 (fn [db _]
   (:map/state db)))

(reg-sub
 :animation/frame-timestamp
 (fn [db _]
   (:animation/frame-timestamp db)))

(reg-sub
 :animation/percentage
 :<- [:animation/frame-timestamp]
 :<- [:analysis/date-range]
 (fn [[ts [df dt]] _]
   (math-utils/calc-perc df dt ts)))

(reg-sub
 :animation/crop
 (fn [db _]
   (:animation/crop db)))

(reg-sub
 :animation/speed
 (fn [db _]
   (:animation/speed db)))

(reg-sub
 :analysis/date-range
 (fn [db _]
   (:analysis/date-range db)))

(reg-sub
 :analysis/highlighted-object-id
 (fn [db _]
   (:analysis/highlighted-object-id db)))

(reg-sub
 :animation/state
 (fn [db _]
   (:animation/state db)))

(reg-sub
 :map/scale
 :<- [:map/state]
 (fn [map-state _]
   (:scale map-state)))

(reg-sub
 :map.state/show-world?
 :<- [:map/state]
 (fn [{:keys [show-world?]}]
   show-world?))

(reg-sub
 :map/data
 :<- [:maps/all-maps-data]
 :<- [:map.state/show-world?]
 (fn [[maps show-world?] _]
   (let [hide-world? (false? show-world?)]
     (geo-json-data-map (cond->> maps
                          hide-world? (remove #(zero? (:map/z-index %))))))))

(reg-sub
 :analysis/data
 (fn [db _]
   (:analysis/data db)))

(defn color-object [obj [attr-key [from to] [color-from color-to]]]

  (if-let [obj-attr-val (get (:attrs obj) (keyword attr-key))]
    (let [perc (/ (- obj-attr-val from)  (- to from))
          color (math-utils/calculate-color color-from color-to perc)]
      (assoc obj :attr-color color))
    (do
      (println "The object " obj "doesn't contain attr " attr-key)
     obj)))

(defn color-data-objects [data obj-type attr]
  (->> data
       (mapv (fn [[obj-id obj]]
               (if (= (:type obj) obj-type)
                 [obj-id (color-object obj attr)]
                 [obj-id obj])))
       (into {})))

(defn filter-pass? [{:keys [attrs]} filter]
  (let [filter-attr (keyword (:attribute/id filter))]
    (if-not (contains? attrs filter-attr)
      true ;; pass thru the filter objects that doesn't contain the attribute

      ;; the object contains the attribute
      (let [obj-val (get attrs filter-attr)]
        (case (:filter/type filter)

          ;; check the obj attribute value is on range
          :linear-filter (let [[rfrom rto] (:range filter)]
                           (<= rfrom obj-val rto))

          ;; check the obj value is in the filter's filter-set
          :ordinal-filter (contains? (:filter-set filter) obj-val))))))

(defn filter-data [data filters]
  (let [attr-filter (fn [obj]
                      (every? (partial filter-pass? obj) filters))]

    (if (empty? filters)
      data
      (utils/filter-map-vals data attr-filter))))

(defn render-sort-data-objects [data-objects]
  (let [obj-order {:polygon 0
                   :circle 1
                   :transition 2
                   :node 3}]
    (->> data-objects
         (sort-by :type (fn [a b] (compare (obj-order a) (obj-order b)))))))

(defn colored-and-filtered-data [filtered-data params]
  (-> (cond
        (:transitions-attribute params) (color-data-objects filtered-data :transition (get params :transitions-attribute))
        (:circles-attribute params)     (color-data-objects filtered-data :circle (get params :circles-attribute))
        (:nodes-attribute params)       (color-data-objects filtered-data :node (get params :nodes-attribute))
        :else filtered-data)))


(reg-sub
  :analysis/filtered-data
  :<- [:analysis/data]
  :<- [:analysis.data/filters]
  (fn [[data filters] _]
    (println "Total data objects " (count data))
    (let [r (filter-data data (vals filters))]
      (println "Filtered data objects " (count r))
      r)))

(reg-sub
  :analysis/filtered-data-sorted
  :<- [:analysis/filtered-data]
  (fn [data _]
    (-> (vals data)
        render-sort-data-objects)))

(reg-sub
  :analysis/colored-and-filtered-data
  :<- [:analysis/filtered-data]
  :<- [:ui/parameters]
  (fn [[filtered-data params] _]
    (colored-and-filtered-data filtered-data params)))

(reg-sub
 :analysis.data/type
 (fn [db _]
   (:analysis.data/type db)))

(reg-sub
 :analysis/selected-object-id
 (fn [db _]
   (:analysis/selected-object-id db)))

(reg-sub
 :analysis/possible-objects-ids
 (fn [db _]
   (:analysis/possible-objects-ids db)))

(reg-sub
 :analysis/selected-object
 :<- [:analysis/data]
 :<- [:analysis/selected-object-id]
 (fn [[objects-map sel-obj-id] _]
   (get objects-map sel-obj-id)))

(reg-sub
 :analysis/possible-objects
 :<- [:analysis/data]
 :<- [:analysis/possible-objects-ids]
 (fn [[objects-map pos-obj-ids] _]
   (vals (select-keys objects-map pos-obj-ids))))

(reg-sub
 :analysis/attributes
 (fn [db _]
   (:analysis/attributes db)))

(reg-sub
 :analysis/linear-attributes
 :<- [:analysis/attributes]
 (fn [attributes _]
   (->> (vals attributes)
        (filter #(= :linear (:attribute/type %)))
        (map (fn [{:keys [id range]}]
               [id range]))
        (into {}))))

(defn attribute-filter? [f]
  (boolean (#{:linear-filter :ordinal-filter} (:filter/type f))))

(reg-sub
 :analysis.data/filters
 (fn [db _]
   (:analysis.data/filters db)))

(reg-sub
 :analysis.data/linear-attribute-filter-range
 :<- [:analysis.data/filters]
 (fn [filters [_ filter-id]]
   (get-in filters [filter-id :range])))

(reg-sub
 :analysis.data/attribute-filters
 :<- [:analysis.data/filters]
 :<- [:analysis/attributes]
 (fn [[filters attributes] _]
   (-> filters
       (utils/filter-map-vals attribute-filter?)
       (utils/map-map-vals (fn [filter]
                             (let [attr (get attributes (:attribute/id filter))]
                               (assoc filter :attribute attr)))))))

(reg-sub
 :map/popup-coord
 (fn [db _]
   (:map/popup-coord db)))

(reg-sub
  :analysis/data-timeline-width
  (fn [db _]
    (:analysis/timeline-width db)))

(reg-sub
  :analysis/data-timeline
  :<- [:analysis/date-range]
  :<- [:analysis/data-timeline-width]
  (fn [[[from-millis to-millis] timeline-px-width]  _]
    (let [start-date (js/Date. from-millis)
          start-month (.getUTCMonth start-date)
          start-year (.getUTCFullYear start-date)
          end-year   (.getUTCFullYear (js/Date. to-millis))
          ticks (->> (range start-year end-year)
                     (mapcat (fn [year]
                               (-> (repeatedly 11 (fn [] {:label nil :type :short}))
                                   (into [{:label (str year) :type :long}]))))
                     (drop start-month)) ;; discard start-month ticks from the biggining since they contain no data
          ticks-cnt (count ticks)
          margin 50
          tick-gap (when (and timeline-px-width (pos? ticks-cnt))
                     (/ (- timeline-px-width margin) (count ticks)))]
      (when tick-gap
        (->> ticks
             (map-indexed (fn [idx tick]
                            (assoc tick :x (* idx tick-gap)))))))))

(defn build-map-parameters [ui-params switch-buttons-states]
  {:poly-fill-color "#ffffff"
   :poly-stroke-color (if (get switch-buttons-states :map-borders?)
                        (get ui-params :map-borders-color "#079DAB")
                        :transparent)
   :poly-stroke-width "0.02"
   :line-color "#B20707"
   :line-width "0.1"
   :text-color (if (get switch-buttons-states :map-labels?)
                 (get ui-params :map-borders-color "#079DAB")
                 :transparent)
   :text-size (:labels-size ui-params)
   :point-color (:nodes-color ui-params)
   :point-radius (:nodes-size ui-params)
   :background-color "#ECEFF8"})

(reg-sub
 :map/parameters
 :<- [:ui/parameters]
 :<- [:switch-buttons/states]
 (fn [[params buttons-states] [_]]
   (build-map-parameters params buttons-states)))

(reg-sub
 :collapsible-tabs/tabs
 (fn [db _]
   (:ui.collapsible-tabs/tabs db)))

(reg-sub
 :switch-buttons/states
 (fn [db _]
   (:ui.switch-buttons/states db)))

(reg-sub
 :switch-buttons/on?
 :<- [:switch-buttons/states]
 (fn [states [_ id]]
   (get states id)))

(reg-sub
 :ui/parameters
 (fn [db [_ param-id]]
   (if param-id
     (get-in db [:ui/parameters param-id])
     (:ui/parameters db))))

(reg-sub
 :parameters/selected
 :<- [:ui/parameters]
 (fn [parameters [_ id]]
   (get parameters id)))

(defn render-params-styles-string [params switch-buttons]
  (let [polygon-stroke (if (:map-borders? switch-buttons) (:map-borders-color params) "transparent")
        data-text-fill (if (:labels? switch-buttons) (:labels-color params) "transparent")
        text-fill (if (:map-labels? switch-buttons) (:labels-color params) "transparent")
        transition-stroke (if (:transitions? switch-buttons) (:transitions-color params) "transparent")
        circle-fill (if (:circles? switch-buttons) (:circles-color params) "transparent")
        node-fill (if (:nodes? switch-buttons) (:nodes-color params) "transparent")]
    (str
      (str "text.label { font-size: " (:labels-size params) "px; fill:" text-fill ";}")
      (str ".data-text { fill : " data-text-fill ";}")
      (str "polygon{ stroke: " polygon-stroke ";}")
      (str ".data-node > circle{ fill: " node-fill "; r: " (:nodes-size params) ";}")
      (str ".data-transition > path { stroke: " transition-stroke "; stroke-width: " (:transitions-width params) ";}")
      (str ".data-polygon > polygon { fill: " (:polygons-color params) "; opacity:" (:polygons-opacity params) ";}")
      (str ".data-circle > circle { fill: " circle-fill ";}"))))

(def hl-color "yellow")

(defn render-elements-styles-string [colored-data high-obj-id]
  (->> colored-data
       (keep (fn [[obj-id {:keys [attr-color type]}]]
               (when-let [color (cond
                                  (= high-obj-id obj-id) hl-color
                                  attr-color             attr-color
                                  :else nil)]
                 (case type
                   :node       (str "#" obj-id " > circle {fill: " color ";}")
                   :circle     (str "#" obj-id " > circle {fill: " color ";}")
                   :transition (str "#" obj-id " > path {stroke: " color ";}")
                   :polygon    (str "#" obj-id " > polygon {fill: " color ";}")))))
       (apply str)))

(defn add-obj-presentation-attrs [obj params]
  (merge obj
         (case (:type obj)
           :transition (let [[x1 y1] (:from-coord obj)
                             [x2 y2] (:to-coord obj)
                             {:keys [f1]} (math-utils/quad-curve-focuses x1 y1 x2 y2 (:transitions-curvature params))
                             [f1x f1y] f1
                             c-length (math-utils/quad-curve-length x1 y1 f1x f1y x2 y2)]
                         {:c-length c-length
                          :f1x f1x :f1y f1y})
           obj)))

(defn calc-obj-time-attrs [{:keys [show-start show-end] :as obj} curr-timestamp params]
  (let [in-range? (<= show-start curr-timestamp show-end)
        show? (or (and (nil? show-start) (nil? show-end)) ;; both nils means show always
                  (if (:missiles? params)
                    in-range?
                    (<= show-start curr-timestamp)))]

    (case (:type obj)
      :node       {:show? (and show? (:nodes? params))
                   :in-change-range? in-range?}
      :circle     {:show? (and show? (:circles? params))
                   :in-change-range? in-range?}
      :polygon    {:show? (and show? (:polygons? params))
                   :in-change-range? in-range?}
      :transition (let [show-trans? (and show? (:transitions? params))
                        c-length (:c-length obj)
                        total-millis (- show-end show-start)
                        delta-t (- curr-timestamp show-start)
                        clip-perc (when show-trans?
                                    (min 1 (/ delta-t total-millis)))
                        dashoffset (- c-length (* c-length clip-perc))]
                    {:show? show-trans?
                     :in-change-range? in-range?
                     :stroke-dashoffset dashoffset
                     :clip-perc clip-perc}))))
