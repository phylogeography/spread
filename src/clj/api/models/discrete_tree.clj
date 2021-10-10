(ns api.models.discrete-tree
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-tree)
(declare get-tree)
(declare insert-attribute)
(declare get-attributes)

(hugsql/def-db-fns "sql/discrete_tree.sql")
(hugsql/def-sqlvec-fns "sql/discrete_tree.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-tree
  {:id                        nil
   :tree-file-url             nil
   :tree-file-name            nil
   :locations-file-url        nil
   :locations-file-name       nil
   :locations-attribute-name  nil
   :timescale-multiplier      nil
   :most-recent-sampling-date nil
   :output-file-url           nil})

(defn upsert! [db {:keys [id] :as tree}]
  (let [prev (or (get-tree db {:id id})
                 nil-tree)
        tree (merge prev tree)]
    (log/debug "upsert discrete tree!" tree)
    (upsert-tree db tree)))

(defn insert-attributes! [db id attributes]
  (log/debug "insert-attributes!" id attributes)
  (doseq [att attributes]
    (insert-attribute db {:id id :attribute-name att})))
