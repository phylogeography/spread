(ns spread.svg-renderer  
  (:require [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.data.json :as json]
            [flow-storm.api :as fsa]
            [clojure.string :as str]
            [spread.maps-utils :as maps-utils]))

(def map-fill-color "#424242")
(def map-stroke-color "pink")
(def background-color "#292929")
(def map-text-color "pink")
(def line-color "orange")
(def data-point-color "#00ffa5")

(defn map-coord->screen-coord [[long lat]]
  [(+ long 180) 
   (+ (* -1 lat) 90) ])

(defn box-intersects? [b1 b2]
  ;; intersects on lat
  (and (or (<= (:min-lat b1) (:min-lat b2) (:max-lat b1))
           (<= (:min-lat b2) (:min-lat b1) (:max-lat b2)))
       ;; intersects on long
       (or (<= (:min-long b1) (:min-long b2) (:max-long b1))
           (<= (:min-long b2) (:min-long b1) (:max-long b2)))))

(defn all-coords

  "Returns the set of all coordinates found in a geo-json map."
  
  [geo-json] 

  (when geo-json
    (case (:type geo-json)
      :Point             #{(:coordinates geo-json)}
      :MultiPoint        (into #{} (:coordinates geo-json))
      :Line              (into #{} (:coordinates geo-json))
      :LineString        (into #{} (:coordinates geo-json))
      :MultiLineString   (into #{} (->> (:coordinates geo-json) (apply concat)))
      :Polygon           (into #{} (->> (:coordinates geo-json) (apply concat)))
      :MultiPolygon      (into #{} (->> (:coordinates geo-json) (apply concat) (apply concat)))
      :FeatureCollection (into #{} (mapcat all-coords (:features geo-json)))
      :Feature           (when-let [g (:geometry geo-json)] (all-coords g))
      (throw (ex-info "Don't know how to find coords of " {:type (:type geo-json)})))))

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

(defn parse-map-file [map-file]
  (json/read-str (slurp map-file)
                 :key-fn keyword
                 :value-fn (fn [k v] (if (= :type k)
                                       (keyword v)
                                       v))))

(defn load-countries-maps [maps-dir]
  (->> maps-dir
       io/file
       file-seq
       (filter #(str/ends-with? (.getName %) "Low.json"))
       (map (fn [file]
              (let [gj-map (parse-map-file (io/file file))]
                (assoc gj-map :map-box (geo-json-bounding-box gj-map)))))))

(defn load-world-map [world-map-file]
  (->> world-map-file
       io/file
       parse-map-file))

(defmulti geojson->svg :type)

(defmethod geojson->svg :Point [{:keys [coordinates]}]
  (let [[long lat] (map-coord->screen-coord coordinates)]
    (xml/element :circle {:cx long :cy lat :r 0.15 :fill data-point-color})))

(defn svg-polygon [coords]
  (let [all-polys (->> coords
                       (mapv (fn [cs]
                               (xml/element
                                :polygon
                                {:points (->> cs
                                              (map (fn [coord]
                                                     (->> 
                                                      (map-coord->screen-coord coord)
                                                      (str/join " "))))
                                              (str/join ","))
                                 :stroke map-stroke-color
                                 :fill map-fill-color
                                 :stroke-width "0.02"}))))]
    (apply xml/element :g {} all-polys)))

(defmethod geojson->svg :Polygon [{:keys [coordinates]}]
  (svg-polygon coordinates))

(defmethod geojson->svg :MultiPolygon [{:keys [coordinates]}]
  (let [all-paths (->> coordinates
                       (map (fn [poly-coords]
                              (svg-polygon poly-coords))))]
   (apply xml/element :g {} all-paths)))

(defn svg-line [[[x1 y1] [x2 y2]]]
  (xml/element :line {:x1 x1 :y1 y1
                      :x2 x2 :y2 y2
                      :stroke line-color
                      :stroke-width "0.1"}))

(defmethod geojson->svg :LineString [{:keys [coordinates]}]
  (svg-line (map map-coord->screen-coord coordinates)))

(defmethod geojson->svg :MultiLineString [{:keys [coordinates]}]
  (let [all-lines (->> coordinates
                       (map (comp svg-line map-coord->screen-coord)))]
   (apply xml/element :g {} all-lines)))

(defn text-for-geo-json [geo-json text]
  (let [{:keys [min-long min-lat max-long max-lat]} (geo-json-bounding-box geo-json)
        [text-x text-y] (map-coord->screen-coord [(+ (/ (Math/abs (- max-long min-long)) 2) min-long)
                                                  (+ (/ (Math/abs (- max-lat min-lat)) 2) min-lat)])]
    (xml/element :text {:x text-x :y text-y
                        :font-size "0.02em" :fill map-text-color
                        :text-anchor "middle"} text)))

(defmethod geojson->svg :Feature [{:keys [geometry properties] :as f}]
  (when geometry
    (let [feature-text (:name properties)]
     (apply xml/element
            :g {}                
            (cond-> [(geojson->svg geometry)]
              feature-text (into [(text-for-geo-json geometry feature-text)]))))))

(defmethod geojson->svg :FeatureCollection [{:keys [features] :as fc}]
  (apply xml/element
         :g {}         
         (mapv geojson->svg features)))

(defmethod geojson->svg :default [x]  
  (throw (ex-info "Not implemented yet" {:type (:type x)})))

(defn view-box-bounding-box

  "Creates a screen bounding box (in screen coordinates) given a `map-box` (in long, lat)
  adding `padding` around."
  
  [map-box padding]

  (let [[x1 y1] (map-coord->screen-coord [(:min-long map-box) (:min-lat map-box)])
        [x2 y2] (map-coord->screen-coord [(:max-long map-box) (:max-lat map-box)])]
    [(- (min x1 x2) (/ padding 2))
     (- (min y1 y2) (/ padding 2))
     (+ (Math/abs (- x1 x2)) padding)
     (+ (Math/abs (- y1 y2)) padding)]))

(def data-box-padding
  "The padding around the databox in the final render."
  5)

(defn render-svg

  "Creates a svg object from  a `geo-json` map." 

  [geo-json]

  (xml/element :svg {:xmlns "http://www.w3.org/2000/svg"
                     :xmlns:amcharts "http://amcharts.com/ammap"
                     :xmlns:xlink "http://www.w3.org/1999/xlink"
                     :version "1.1"
                     :width "100%"
                     :height "100%"}
               (xml/element :rect {:x "0" :y "0" :width "100%" :height "100%" :fill background-color})
               (xml/element
                :svg {:viewBox (apply format "%f %f %f %f" (view-box-bounding-box (:map-box geo-json)
                                                                                  data-box-padding))}                
                (geojson->svg geo-json))))

(defn write-svg-file

  "Writes `svg` xml object into `file-path`."

  [svg file-path]
  (with-open [out-file (java.io.FileWriter. file-path)]
    (xml/emit svg out-file)))

(def world-map-long-threshhold
  "When data bounding box exceeds this longitude difference
  we switch from detailed countries maps to world map."
  80)

(def world-map-lat-threshhold
  "When data bounding box exceeds this latitude difference
  we switch from detailed countries maps to world map."
  40)

(def countries-maps (load-countries-maps (io/resource "maps/countries")))
(def world-map      (load-world-map (io/resource "maps/world/worldLow.json")))

(defn make-svg
  
  "Given `data` as a geo-json map and `svg-file-path` render a svg
  that contains `data` on top of the minimal map needed to cover its bounding box."
  
  [data svg-file-path]
  (let [data-box (geo-json-bounding-box data)
        world? (or (> (Math/abs (- (:max-long data-box) (:min-long data-box))) world-map-long-threshhold)
                   (> (Math/abs (- (:max-lat data-box) (:min-lat data-box))) world-map-lat-threshhold))
        composed-geo-json (if world?
                            
                            {:type :FeatureCollection
                             :features (into (:features world-map) (:features data))
                             :map-box data-box}

                            (let [relevant-maps (->> countries-maps
                                                     (filter (fn [{:keys [map-box]}]
                                                               (box-intersects? map-box data-box)))
                                                     (into []))]
                              {:type :FeatureCollection
                               :features (into relevant-maps (:features data))
                               :map-box data-box}))]
    (-> composed-geo-json    
        render-svg
        (write-svg-file svg-file-path))))


;;;;;;;;;;;;;;
;; Examples ;;
;;;;;;;;;;;;;;

(comment

  (def example-data
    (let [brazil [-51.619789 -9.588903]
          poland [19.300636 52.12461]
          uruguay  [-56.012396 -32.799646]
          germany [10.018343 51.133481]]
      {:type :FeatureCollection
       :features [{:type :Feature
                   :geometry {:type :Point
                              :coordinates uruguay}}
                  {:type :Feature
                   :geometry {:type :Point
                              :coordinates brazil}}
                  {:type :Feature
                   :geometry {:type :LineString
                              :coordinates [uruguay brazil]}}
                  
                  {:type :Feature
                   :geometry {:type :Point
                              :coordinates germany}}
                  {:type :Feature
                   :geometry {:type :Point
                              :coordinates poland}}
                  
                  
                  {:type :Feature
                   :geometry {:type :LineString
                              :coordinates [uruguay germany]}}
                  {:type :Feature
                   :geometry {:type :LineString
                              :coordinates [germany poland]}}
                  ]}))

  (make-svg example-data "./composed.svg")
  
  (def cont-data (maps-utils/load-spread-data-as-geo-json "docs/data_examples/continuous_parsed.json"))
  (make-svg cont-data "./composed.svg")

  ;; TODO: make maps-utils/load-spread-data-as-geo-json work for discreate
  #_(def disc-data (maps-utils/load-spread-data-as-geo-json "docs/data_examples/discrete_parsed.json"))
  #_(make-svg disc-data "./composed.svg")
  
  (fsa/connect {:tap-name "maps"})

  
  )

