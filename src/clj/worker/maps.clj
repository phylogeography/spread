(ns worker.maps
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [shared.math-utils :as math-utils]
            [shared.output-data :as output-data]
            [shared.utils :as shared-utils]))

(def max-url-details-map-count 7)

(defn load-map-boxes []
  (-> (io/resource "maps/maps-boxes.edn")
      slurp
      read-string))

(defn maps-country-codes-for-data [output-json map-boxes]
  (let [data-box (output-data/data-bounding-box output-json)]
    (->> map-boxes
        (filter (fn [{:keys [box]}]
                  (math-utils/box-overlap? (set/rename-keys box {:min-lon :min-x
                                                                 :min-lat :min-y
                                                                 :max-lon :max-x
                                                                 :max-lat :max-y})
                                           data-box)))
        (map :country/code))))

(defn build-maps-url-param [codes]
  (->> codes
       (take max-url-details-map-count)
       (str/join ",")
       (format "maps=%s")))

(defn build-viewer-url-params [user-id analysis-id map-boxes output-json]
  (let [suggested-codes (maps-country-codes-for-data output-json map-boxes)
        output-param (format "output=%s/%s.json" user-id analysis-id)
        maps-param (build-maps-url-param suggested-codes)]
    (format "?%s&%s" output-param maps-param)))

(comment

  (def map-boxes (load-map-boxes))

  (def output (-> "/home/jmonetta/tmp/discrete-output.json"
                  slurp
                  shared-utils/decode-json))

  ;; TODO: figure out integration
  ;; I think optimally we should do it at worker time and save the maps codes into the db
  ;; only once after generating the output file
  ;; For that we need to call (load-map-boxes) at worker start use build-maps-url-param at
  ;; api level to build the url for the analysis or just make the worker store the full url in the db

  (def suggested-codes (maps-country-codes-for-data output map-boxes)) ;; => ("CN" "HK" "VN")

  (build-maps-url-param suggested-codes) ;; => "maps=CN,HK,VN"

  )
