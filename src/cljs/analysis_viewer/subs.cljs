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

(defn colored-and-filtered-data [data params filters]
  (println "Total data objects " (count data))
  (let [filtered-data (filter-data data (vals filters))
        _ (println "Filtered data objects " (count filtered-data))
        final-data (cond
                     (:transitions-attribute params) (color-data-objects filtered-data :transition (get params :transitions-attribute))
                     (:circles-attribute params)     (color-data-objects filtered-data :circle (get params :circles-attribute))
                     (:nodes-attribute params)       (color-data-objects filtered-data :node (get params :nodes-attribute))
                     :else filtered-data)]
    final-data))

(reg-sub
 :analysis/colored-and-filtered-data
 :<- [:analysis/data]
 :<- [:ui/parameters]
 :<- [:analysis.data/filters]
 (fn [[data params filters] _]
   (colored-and-filtered-data data params filters)))

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
 :analysis/data-timeline
 :<- [:analysis/date-range]
 (fn [[from-millis to-millis] _]
   (let [tick-gap 7
         start-date (js/Date. from-millis)
         start-month (.getUTCMonth start-date)
         start-year (.getUTCFullYear start-date)
         end-year   (.getUTCFullYear (js/Date. to-millis))]
     (->> (range start-year end-year)
          (mapcat (fn [year]
                    (-> (repeatedly 11 (fn [] {:label nil :type :short}))
                        (into [{:label (str year) :type :long}]))))
          (drop start-month) ;; discard start-month ticks from the biggining since they contain no data
          (map-indexed (fn [idx tick]
                         (assoc tick :x (* idx tick-gap))))))))

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
