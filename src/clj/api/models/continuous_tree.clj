(ns api.models.continuous-tree
  (:require [hugsql.core :as hugsql]))

;; This are just so clj-kondo doesn't complain
(declare upsert-tree)

(hugsql/def-db-fns "sql/continuous_tree.sql")
(hugsql/def-sqlvec-fns "sql/continuous_tree.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-tree
  {
   ;; :tree-id nil
   ;; :user-id nil
   ;; :tree-file-url nil
   :x-coordinate-attribute-name nil
   :y-coordinate-attribute-name nil
   :hpd-level nil
   :has-external-annotations nil
   :timescale-multiplier nil
   :most-recent-sampling-date nil
   }

  )

(defn upsert-tree! [db tree]
  (upsert-tree db (merge nil-tree tree)))
