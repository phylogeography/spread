(ns spread.utils)

(defn deep-merge
  "Merge nested values from left to right.
   Examples:
   (deep-merge {:a {:b {:c 3}}}
               {:a {:b 3}})
   => {:a {:b 3}}"
  ([] nil)
  ([m] m)
  ([m1 m2]
   (reduce-kv (fn [out k v]
                (let [v1 (get out k)]
                  (cond (nil? v1)
                        (assoc out k v)

                        (and (map? v) (map? v1))
                        (assoc out k (deep-merge v1 v))

                        (= v v1)
                        out

                        :else
                        (assoc out k v))))
              m1
              m2))
  ([m1 m2 & ms]
   (apply deep-merge (deep-merge m1 m2) ms)))

(defn random-uuid []
  (str (java.util.UUID/randomUUID)))
