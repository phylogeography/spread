(ns analysis-viewer.scripts
  (:require [api.config :as config]
            [aws.s3 :as aws-s3]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [shared.geojson :as geojson]
            [shared.utils :as shared-utils]))

;; Run like
;; clj -X analysis-viewer.scripts/ensure-maps-uploaded
(defn ensure-maps-uploaded
  "For every map file in resources check if it is already in our s3 bucket.
  Upload it if not."
  [_]
  (let [{:keys [bucket-name] :as aws-config} (:aws (config/load!))        
        s3 (aws-s3/create-client aws-config)
        maps-files (into [] (.listFiles (io/file (io/resource "maps/country-code-maps"))))]
    (println (format "Ensuring %d maps are uploaded to s3 using config :" (count maps-files)))
    (prn aws-config)
    (doseq [map-file maps-files]
      (let [map-rel-path (-> map-file str (string/split #"resources/") second)
            map-url (str "http://" (aws-s3/build-url aws-config bucket-name map-rel-path))]
        (when-not (shared-utils/http-file-exists? map-url)
          (println (str "Map file " map-url " doesn't exist, uploading..."))
          (aws-s3/upload-file s3 {:bucket bucket-name
                                  :key map-rel-path
                                  :file-path (str map-file)}))))))

;; Run like
;; clj -X analysis-viewer.scripts/gen-maps-boxes-file
(defn gen-maps-boxes-file
  "Generate a file containing a collection of bounding boxes
  for each map inside resources/maps/country-code-maps"
  [_]
  (let [output "resources/maps/maps-boxes.edn"
        maps-files (into [] (.listFiles (io/file (io/resource "maps/country-code-maps"))))
        boxes (->> maps-files
                   (keep (fn [f]
                           (let [box (-> (slurp f)
                                         shared-utils/decode-json
                                         geojson/geo-json-bounding-box)
                                 [_ country-code] (re-find #".+/(.+)\.json" (.getAbsolutePath f))]
                             
                             (when-not (string/includes? country-code "WORLD") ;; skip world map
                               {:country/code country-code
                                :box (set/rename-keys box {:min-x :min-lon
                                                           :min-y :min-lat
                                                           :max-x :max-lon
                                                           :max-y :max-lat})})))))
        
        boxes-str (->> boxes
                       (map (fn [b]
                              (let [{:keys [min-lon min-lat max-lon max-lat]} (:box b)]
                                (when-not (and min-lon  max-lon min-lat  max-lat
                                               (<= -180 min-lon max-lon 180)
                                               (<= -90  min-lat max-lat 90))
                                  (print "WARNING: Box boundries doesn't look good ")
                                  (prn b))                                
                                (str b "\n"))))
                       (apply str))]
    (spit output (format "[%s]" boxes-str))
    (println (format "Done, wrote %d boxes in %s" (count boxes) output))))
