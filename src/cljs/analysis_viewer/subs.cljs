(ns analysis-viewer.subs
  (:require [analysis-viewer.svg-renderer :as svg-renderer]
            [re-frame.core :refer [reg-sub]]))

(defn geo-json-data-map [db-maps]
  (let [maps {:type "FeatureCollection"
              :features (->> db-maps
                             (sort-by :map/z-index <)
                             (map :map/geo-json))}]
    (assoc maps :map-box (svg-renderer/geo-json-bounding-box maps))))

(reg-sub
 :maps/all-maps-data
 (fn [db _]
  (:maps/data db)))

(reg-sub
 :map/state
 (fn [db _]
   (:map/state db)))

(reg-sub
 :animation/percentage
 (fn [db _]
   (:animation/percentage db)))

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

(reg-sub
 :analysis/data-timeline
 :<- [:analysis/data]
 (fn [_ _]
   (let [tick-gap 10
         ticks-x-base 10]
     (->> (range 2011 2020)
          (mapcat (fn [year]
                    (-> (repeatedly 11 (fn [] {:label nil :type :short :perc (rand-int 100)}))
                        (into [{:label (str year) :type :long :perc (rand-int 100)}]))))
          (map-indexed (fn [idx tick]
                         (assoc tick :x (+ (* idx tick-gap) ticks-x-base))))))))

(reg-sub
 :map/parameters
 :<- [:ui/parameters]
 :<- [:switch-buttons/states]
 (fn [[params buttons-states] [_]]
   {:map-fill-color "#ffffff"
    :background-color "#ECEFF8"
    :map-stroke-color (if (get buttons-states :map-borders)
                        (get params :map-borders-color "#079DAB")
                        :transparent)
    :map-text-color (get params :map-borders-color "#079DAB")                     
    :line-color "#B20707"
    :data-point-color "#DD0808"}))

(reg-sub
 :collapsible-tabs/tabs
 (fn [db _]
   (:ui.collapsible-tabs/tabs db)))

(reg-sub
 :collapsible-tabs/open?
 :<- [:collapsible-tabs/tabs]
 (fn [tabs [_ parent-id tab-id]]
   (get-in tabs [parent-id tab-id])))

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
