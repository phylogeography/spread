(ns analysis-viewer.svg-renderer
  "
  Render svg hiccup structure from geo-json maps.
  Api :
  - geojson->svg
  "
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [shared.geojson :as geojson]
            [shared.math-utils :as math-utils]))

(def ^:dynamic *coord-transform-fn* identity)

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

;; (s/fdef geojson->svg
;;   :args (s/cat :gjson ::geojson
;;                :opts ::opts)
;;   :ret ::svg)

(declare geojson->svg)

(defn geojson->svg-point [{:keys [coordinates]} opts]
  (let [[long lat] (*coord-transform-fn* coordinates)]
    [:circle {:cx long :cy lat :r (:point-radius opts) :fill (:data-point-color opts)}]))

(defn svg-polygon [coords opts]
  (let [all-polys (->> coords
                       (mapv (fn [cs]
                               [:polygon
                                {:points (->> cs
                                              (mapv (fn [coord]
                                                      (->> (*coord-transform-fn* coord)
                                                           (str/join " "))))
                                              (str/join ","))
                                 :stroke (:poly-stroke-color opts)
                                 :fill (:poly-fill-color opts)
                                 :stroke-width (:poly-stroke-width opts)}])))]
    (into [:g {}] all-polys)))

(defn geojson->svg-polygon [{:keys [coordinates]} opts]
  (svg-polygon coordinates opts))

(defn geojson->svg-multi-polygon [{:keys [coordinates]} opts]
  (let [all-paths (->> coordinates
                       (map (fn [poly-coords]
                              (svg-polygon poly-coords opts))))]
    (into [:g {}] all-paths)))

(defn svg-line [[[x1 y1] [x2 y2]] opts]
  [:line {:x1 x1 :y1 y1
          :x2 x2 :y2 y2
          :stroke (:line-color opts)
          :stroke-width (:line-width opts)}])

(defn geojson->svg-line-string [{:keys [coordinates]} opts]
  (svg-line (map *coord-transform-fn* coordinates) opts))

(defn geojson->svg-multi-line-string [{:keys [coordinates]} opts]
  (let [all-lines (->> coordinates
                       (map (fn [coor]
                              (svg-line (*coord-transform-fn* coor) opts))))]
    (into [:g {} all-lines])))

(defn text-for-box [box text opts]
  (let [{:keys [min-x min-y max-x max-y]} box
        [x1 y1] (*coord-transform-fn* [min-x min-y])
        [x2 y2] (*coord-transform-fn* [max-x max-y])
        [text-x text-y] [(+ (/ (Math/abs (- x1 x2)) 2) (min x1 x2))
                         (+ (/ (Math/abs (- y1 y2)) 2) (min y1 y2))]]
    [:text {:x text-x :y text-y
            ;; :font-size (str (:text-size opts) "px")
            :fill (:text-color opts)
            :text-anchor "middle"} text]))

(defn geojson->svg-feature [{:keys [geometry properties]} opts]
  (when geometry
    (let [geo-box (geojson/geo-json-bounding-box geometry) ;; this is in [long lat]
          feature-text (:name properties)]

      (when (or (nil? (:clip-box opts))
                (math-utils/box-overlap? (:clip-box opts)
                                         (math-utils/map-box->proj-box geo-box)))

        (into [:g {}] (cond-> [(geojson->svg geometry opts)]
                        feature-text (into [(text-for-box geo-box feature-text opts)])))))))

(defn geojson->svg-feature-collection [{:keys [features]} opts]
  (into [:g {}] (mapv (fn [feat] (geojson->svg feat opts)) features)))

(defn geojson->svg-default [x _]
  (throw (ex-info "Not implemented yet" {:type (:type x)})))

(defn geojson->svg [{:keys [type] :as geoj} opts]
  (case (keyword type)
    :Point             (geojson->svg-point geoj opts)
    :Polygon           (geojson->svg-polygon geoj opts)
    :MultiPolygon      (geojson->svg-multi-polygon geoj opts)
    :LineString        (geojson->svg-line-string geoj opts)
    :MultiLineString   (geojson->svg-multi-line-string geoj opts)
    :Feature           (geojson->svg-feature geoj opts)
    :FeatureCollection (geojson->svg-feature-collection geoj opts)
    (geojson->svg-default geoj opts)
    ))
