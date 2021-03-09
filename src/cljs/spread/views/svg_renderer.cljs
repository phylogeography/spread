(ns spread.views.svg-renderer
  "
  Render svg hiccup structure from get-json maps.
  Api :
  - geojson->svg
  "
  (:require [clojure.string :as str]
            [spread.math-utils :as math-utils]))


(def ^:dynamic *theme* {:map-fill-color "#424242"
                        :map-stroke-color "pink"
                        :map-text-color "pink"
                        :line-color "orange"
                        :data-point-color "#00ffa5"})

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

(defn geo-json-bounding-box

  "Calculates the bounding box (in long,lat) for any geo-json map."
  
  [geo-json]

  (let [coords (all-coords geo-json)]
    (loop [min-lat 90
           min-long 180
           max-lat -90
           max-long -180
           [[lon lat] & rcoords] coords]
      (if lat
        (recur (min min-lat lat)
               (min min-long lon)
               (max max-lat lat)
               (max max-long lon)
               rcoords)
        {:min-lat min-lat
         :min-long min-long
         :max-lat max-lat
         :max-long max-long}))))

(defmulti geojson->svg (fn [{:keys [type]}] (keyword type)))

(defmethod geojson->svg :Point [{:keys [coordinates]}]
  (let [[long lat] (math-utils/map-coord->proj-coord coordinates)]
    [:circle {:cx long :cy lat :r 0.1 :fill (:data-point-color *theme*)}]))

(defn svg-polygon [coords]
  (let [all-polys (->> coords
                       (mapv (fn [cs]
                               [:polygon
                                {:points (->> cs
                                              (map (fn [coord]
                                                     (->> (math-utils/map-coord->proj-coord coord)
                                                          (str/join " "))))
                                              (str/join ","))
                                 :stroke (:map-stroke-color *theme*)
                                 :fill (:map-fill-color *theme*)
                                 :stroke-width "0.02"}])))]
    (into [:g {}] all-polys)))

(defmethod geojson->svg :Polygon [{:keys [coordinates]}]
  (svg-polygon coordinates))

(defmethod geojson->svg :MultiPolygon [{:keys [coordinates]}]
  (let [all-paths (->> coordinates
                       (map (fn [poly-coords]
                              (svg-polygon poly-coords))))]
    (into [:g {}] all-paths)))

(defn svg-line [[[x1 y1] [x2 y2]]]
  [:line {:x1 x1 :y1 y1
          :x2 x2 :y2 y2
          :stroke (:line-color *theme*)
          :stroke-width "0.1"}])

(defmethod geojson->svg :LineString [{:keys [coordinates]}]
  (svg-line (map math-utils/map-coord->proj-coord coordinates)))

(defmethod geojson->svg :MultiLineString [{:keys [coordinates]}]
  (let [all-lines (->> coordinates
                       (map (comp svg-line math-utils/map-coord->proj-coord)))]
    (into [:g {} all-lines])))

(defn text-for-geo-json [geo-json text]
  (let [{:keys [min-long min-lat max-long max-lat]} (geo-json-bounding-box geo-json)
        [text-x text-y] (math-utils/map-coord->proj-coord [(+ (/ (Math/abs (- max-long min-long)) 2) min-long)
                                                           (+ (/ (Math/abs (- max-lat min-lat)) 2) min-lat)])]
    [:text {:x text-x :y text-y
            :font-size "0.02em" :fill (:map-text-color *theme*)
            :text-anchor "middle"} text]))

(defmethod geojson->svg :Feature [{:keys [geometry properties] :as f}]
  (when geometry
    (let [feature-text (:name properties)]
      (into [:g {}] (cond-> [(geojson->svg geometry)]
                      feature-text (into [(text-for-geo-json geometry feature-text)]))))))

(defmethod geojson->svg :FeatureCollection [{:keys [features] :as fc}]
  (into [:g {}] (mapv geojson->svg features)))

(defmethod geojson->svg :default [x]  
  (throw (ex-info "Not implemented yet" {:type (:type x)})))


