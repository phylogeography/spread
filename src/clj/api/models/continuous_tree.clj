(ns api.models.continuous-tree
  (:require [hugsql.core :as hugsql]))

;; This are just so clj-kondo doesn't complain
(declare upsert-video)

(hugsql/def-db-fns "sql/continuous_tree.sql")
(hugsql/def-sqlvec-fns "sql/continuous_tree.sql")

(defn upsert-tree! [db tree]
  (upsert-tree db tree))
