(ns api.scalars)

(defn parse-big-int
  [string]
  (bigint string))

(defn serialize-big-int
  [big-int]
  (str big-int))
