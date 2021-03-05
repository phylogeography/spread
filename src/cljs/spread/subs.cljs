(ns spread.subs
  (:require [re-frame.core :as re-frame]
            [spread.views.svg-renderer]
            [spread.math-utils :as math-utils]))

(re-frame/reg-sub
 ::map-data
 (fn [db _] 
   (let [maps {:type "FeatureCollection"
               :features (->> (:maps db)                              
                              (sort-by :map/z-index <)
                              (map :map/geo-json))}]     
     (assoc maps :map-box (spread.views.svg-renderer/geo-json-bounding-box maps)))))

(re-frame/reg-sub
 ::map-view-box
 (fn [{:keys [map-view-box-center map-view-box-radius]} _]
   (math-utils/outscribing-rectangle map-view-box-center map-view-box-radius)))

(re-frame/reg-sub
 ::data-points
 (fn [db _] 
   (:data-points db)))

(re-frame/reg-sub
 ::map-state
 (fn [db _] 
   (:map-state db)))

