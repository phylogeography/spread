(ns api.models.custom-map
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-custom-map)
(declare get-custom-map)
(declare delete-custom-map)

(hugsql/def-db-fns "sql/custom_map.sql")
(hugsql/def-sqlvec-fns "sql/custom_map.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-custom-map
  {:analysis-id nil
   :fine-name   nil
   :file-url    nil})

(defn upsert! [db {:keys [analysis-id] :as custom-map}]
  (let [prev     (or (get-custom-map db {:analysis-id analysis-id})
                     nil-custom-map)
        custom-map (merge prev custom-map)]
    (log/debug "upsert-custom-map" custom-map)
    (upsert-custom-map db custom-map)))
