(ns shared.utils)

(defn filter-map-vals
  "Filter `m` entries where (pred val) is true"
  [m pred]
  (->> m
       (keep (fn [[k v]]
               (when (pred v )
                 [k v])))
       (into {})))

(defn map-map-vals
  "Transform `m` by applying f to each map value"
  [m f]
  (->> m
       (map (fn [[k v]] [k (f v)]))
       (into {})))
