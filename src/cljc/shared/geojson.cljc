(ns shared.geojson
  (:require [shared.math-utils :as math-utils]))

(defn all-coords

  "Returns the set of all coordinates found in a geo-json map."
  
  [geo-json] 

  (when geo-json
    (case (keyword (:type geo-json))
      :Point             #{(:coordinates geo-json)}
      :MultiPoint        (into #{} (:coordinates geo-json))
      :Line              (into #{} (:coordinates geo-json))
      :LineString        (into #{} (:coordinates geo-json))
      :MultiLineString   (into #{} (->> (:coordinates geo-json) (apply concat)))
      :Polygon           (into #{} (->> (:coordinates geo-json) (apply concat)))
      :MultiPolygon      (into #{} (->> (:coordinates geo-json) (apply concat) (apply concat)))
      :FeatureCollection (into #{} (mapcat all-coords (:features geo-json)))
      :Feature           (when-let [g (:geometry geo-json)] (all-coords g))
      (throw (ex-info (pr-str "Don't know how to find coords of " (:type geo-json)) {})))))

;; This doesn't change since geo-json objects we are dealing with are immutable
;; so we can memoize it
(def geo-json-bounding-box 

  "Calculates the bounding box in proj-coord for any geo-json map."
  
  (memoize
    (fn [geo-json]

     (let [coords (->> (all-coords geo-json))]
       (math-utils/bounding-box coords)))))
