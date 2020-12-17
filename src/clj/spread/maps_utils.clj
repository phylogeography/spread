(ns spread.maps-utils
  (:require [clojure.data.json :as json]))

(defn point [index {:keys [id coordinate]}]
  {:type :Point
   :coordinates [(:xCoordinate coordinate) (:yCoordinate coordinate)]})

(defn line [index {:keys [startPointId endPointId]}]
  (let [{start-long :xCoordinate start-lat :yCoordinate} (:coordinate (get-in index [:points startPointId]))
        {end-long :xCoordinate end-lat :yCoordinate} (:coordinate (get-in index [:points endPointId]))]    
    {:type :LineString
     :coordinates [[start-long start-lat] [end-long end-lat]]}))

(defn area [{:keys [polygon] :as area}]
  {:type :Polygon
   :points (mapv (fn [{:keys [xCoordinate yCoordinate]}] [xCoordinate yCoordinate]) polygon)})

(defn layer-features [index {:keys [points lines areas] :as layer}]
  (->> (map (partial point index) points)
       (into (map (partial line index) lines))
       (into (map area areas))
       (mapv (fn [geometry]
               {:type :Feature
                :geometry geometry}))))

(defn build-index [{:keys [layers] :as data}]
  (let [layer (first layers)] ;; TODO: figure out how layers wors, are all of them of type tree?
    {:points (reduce (fn [r {:keys [id] :as point}]
                       (assoc r id point))
                     {}
                     (:points layer))}))

(defn spread-data->geo-gson
  
  "Take continuous or discrete json data and returns geo-json"
  
  [{:keys [layers] :as data}]

  (let [index (build-index data)
        features (mapcat (partial layer-features index) layers)]
    {:type :FeatureCollection
     :features features}))

(defn load-spread-data-as-geo-json [file-path]
  (-> (slurp file-path)
      (json/read-str :key-fn keyword)
      spread-data->geo-gson))

(comment
  (require '[flow-storm.api :as fsa])
  (fsa/connect {:tap-ref "spread"})
  (def cont-data (-> (slurp "docs/data_examples/continuous_parsed.json")
                     (json/read-str :key-fn keyword)))
  (def disc-data (-> (slurp "docs/data_examples/discrete_parsed.json")
                     (json/read-str :key-fn keyword)))

  (def gj (spread-data->geo-gson cont-data))
  )
