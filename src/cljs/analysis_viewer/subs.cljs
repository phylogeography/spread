(ns analysis-viewer.subs
  (:require [analysis-viewer.svg-renderer :as svg-renderer]
            [re-frame.core :refer [reg-sub]]
            [shared.math-utils :as math-utils]))

(defn geo-json-data-map [db-maps]
  (let [maps {:type "FeatureCollection"
              :features (->> db-maps
                             (sort-by :map/z-index <)
                             (map :map/geo-json))}]
    (assoc maps :map-box (svg-renderer/geo-json-bounding-box maps))))

(reg-sub
 ::maps
 (fn [db _]
  (:maps db)))

(reg-sub
 ::map-state
 (fn [db _]
   (:map-state db)))

(reg-sub
 ::map-state-show
 :<- [::map-state]
 (fn [{:keys [show-world?]}]
   show-world?))

(reg-sub
 ::map-data
 :<- [::maps]
 :<- [::map-state-show]
 (fn [[maps show-world?] _]
   (let [hide-world? (false? show-world?)]
     (geo-json-data-map (cond->> maps
                          hide-world? (remove #(zero? (:map/z-index %))))))))

(reg-sub
 ::map-view-box
 (fn [{:keys [map-view-box-center map-view-box-radius]} _]
   (math-utils/outscribing-rectangle map-view-box-center map-view-box-radius)))

(reg-sub
 ::analysis-data
 (fn [db _]
   (:analysis-data db)))


