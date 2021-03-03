(ns spread.subs
  (:require [re-frame.core :as re-frame]
            [spread.views.svg-renderer]))

(re-frame/reg-sub
 ::maps
 (fn [db _] 
   (let [maps {:type "FeatureCollection"
               :features (:maps db)}]     
     (assoc maps :map-box (spread.views.svg-renderer/geo-json-bounding-box maps)))))

