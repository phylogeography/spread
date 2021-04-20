(ns analysis-viewer.scripts
  (:require [aws.s3 :as aws-s3]
            [api.config :as config]
            [clojure.java.io :as io]
            [clojure.string :as string]
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




