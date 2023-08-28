(ns ui.new-analysis.file-formats
  (:require [clojure.string :as str]))

;; see db.changelog-8.0.xml
(def ^:const name-length-limit 50)

(defn b->Mb [b]
  (-> b (/ 1024) (/ 1024)))

(defn tree-file-accept-predicate [{:keys [file size]}]
  (-> (.text (.slice file 0 6))
      (.then (fn [first-bytes-str]
               (and (<= (count (.-name file)) name-length-limit)
                    (= first-bytes-str "#NEXUS")
                    (<= (b->Mb size) 150))))))

(def trees-file-accept-predicate tree-file-accept-predicate)

(defn log-file-accept-predicate [{:keys [file]}]
  (let [log-head-size 3000]
    (-> (.text (.slice file 0 log-head-size))
        (.then (fn [file-head-str]
                 ;; `file-head-str` containts 3Kb of the log file
                 ;; The acceptance criteria is that the first row should contain all numbers
                 ;; First row is counted after skipping comments and headers
                 (let [first-row (->> (str/split-lines file-head-str)
                                      (drop-while #(str/starts-with? % "#")) ;; drop the comments
                                      rest ;; drop the headers
                                      first)]
                   (and (<= (count (.-name file)) name-length-limit)
                        (->> (str/split first-row #"[\t\s]")
                             (remove str/blank?)
                             (every? #(not (js/isNaN (js/parseFloat %))))))))))))

(defn locations-file-accept-predicate [{:keys [file]}]
  (-> (.text file) ;; this will load the entire coordinates file in memory but they look pretty small (~1Kb)
      (.then (fn [file-content]
               (try
                 (let [first-line       (-> file-content
                                      (str/split-lines)
                                      first)
                       [location n1 n2] (-> first-line
                                            (str/split #"[\t\s]"))]
                   (and (<= (count (.-name file)) name-length-limit)
                        (string? location)
                        (not (js/isNaN (js/parseFloat n1)))
                        (not (js/isNaN (js/parseFloat n2)))))
                 (catch js/Error _ false))))))

(defn custom-map-file-accept-predicate [{:keys [file]}]
  (-> (.text file)
      (.then (fn [file-content]
               (try
                 (let [map-json (js/JSON.parse file-content)]
                   (and (<= (count (.-name file)) 200) ;; see db.changelog-9.0.xml
                     (boolean (#{"FeatureCollection" "Feature"} (.-type map-json)))))
                 (catch js/Error _ false))))))
