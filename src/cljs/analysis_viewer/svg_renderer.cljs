(ns analysis-viewer.svg-renderer
  "
  Render svg hiccup structure from geo-json maps.
  Api :
  - geojson->svg
  "
  (:require [clojure.string :as str]
            [shared.math-utils :as math-utils]
            [clojure.spec.alpha :as s]))

(def ^:dynamic *coord-transform-fn* identity)

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

(s/def ::geojson any?)
(s/def :html/color string?)
(s/def ::poly-stroke-color :html/color)
(s/def ::poly-fill-color :html/color)
(s/def ::poly-stroke-width number?)
(s/def ::point-color :html/color)
(s/def ::point-radius number?)
(s/def ::line-color :html/color)
(s/def ::line-width number?)
(s/def ::text-color :html/color)
(s/def ::text-size number?)

(s/def ::opts (s/keys :opt-un [::poly-stroke-color
                               ::poly-fill-color
                               ::poly-stroke-width
                               ::point-color
                               ::point-radius
                               ::line-color
                               ::line-width
                               ::text-color]))
(s/def ::svg any?)

(s/fdef geojson->svg
  :args (s/cat :gjson ::geojson
               :opts ::opts)
  :ret ::svg)

(defmulti geojson->svg (fn [{:keys [type]} _] (keyword type)))

(defmethod geojson->svg :Point [{:keys [coordinates]} opts]
  (let [[long lat] (*coord-transform-fn* coordinates)]
    [:circle {:cx long :cy lat :r (:point-radius opts) :fill (:data-point-color opts)}]))

(defn svg-polygon [coords opts]  
  (let [all-polys (->> coords
                       (mapv (fn [cs]
                               [:polygon
                                {:points (->> cs
                                              (map (fn [coord]
                                                     (->> (*coord-transform-fn* coord)
                                                          (str/join " "))))
                                              (str/join ","))
                                 :stroke (:poly-stroke-color opts)
                                 :fill (:poly-fill-color opts)
                                 :stroke-width (:poly-stroke-width opts)}])))]
    (into [:g {}] all-polys)))

(defmethod geojson->svg :Polygon [{:keys [coordinates]} opts]
  (svg-polygon coordinates opts))

(defmethod geojson->svg :MultiPolygon [{:keys [coordinates]} opts]
  (let [all-paths (->> coordinates
                       (map (fn [poly-coords]
                              (svg-polygon poly-coords opts))))]
    (into [:g {}] all-paths)))

(defn svg-line [[[x1 y1] [x2 y2]] opts]
  [:line {:x1 x1 :y1 y1
          :x2 x2 :y2 y2
          :stroke (:line-color opts)
          :stroke-width (:line-width opts)}])

(defmethod geojson->svg :LineString [{:keys [coordinates]} opts]
  (svg-line (map *coord-transform-fn* coordinates) opts))

(defmethod geojson->svg :MultiLineString [{:keys [coordinates]} opts]
  (let [all-lines (->> coordinates
                       (map (fn [coor]
                              (svg-line (*coord-transform-fn* coor) opts))))]
    (into [:g {} all-lines])))

(defn text-for-geo-json [geo-json text opts]
  (let [{:keys [min-long min-lat max-long max-lat]} (geo-json-bounding-box geo-json)
        [text-x text-y] (*coord-transform-fn* [(+ (/ (Math/abs (- max-long min-long)) 2) min-long)
                                               (+ (/ (Math/abs (- max-lat min-lat)) 2) min-lat)])]
    [:text {:x text-x :y text-y
            :font-size (str (:text-size opts) "px")
            :fill (:text-color opts)
            :text-anchor "middle"} text]))

(defmethod geojson->svg :Feature [{:keys [geometry properties]} opts]
  (when geometry
    (let [feature-text (:name properties)]
      (into [:g {}] (cond-> [(geojson->svg geometry opts)]
                      feature-text (into [(text-for-geo-json geometry feature-text opts)]))))))

(defmethod geojson->svg :FeatureCollection [{:keys [features]} opts]
  (into [:g {}] (mapv (fn [feat] (geojson->svg feat opts)) features)))

(defmethod geojson->svg :default [x _]  
  (throw (ex-info "Not implemented yet" {:type (:type x)})))
