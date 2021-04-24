(ns api.models.discrete-tree
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-tree)
(declare update-tree)
(declare insert-attribute)
(declare get-tree)
(declare delete-tree)
(declare get-attributes)
(declare upsert-status)
(declare get-status)

(hugsql/def-db-fns "sql/discrete_tree.sql")
(hugsql/def-sqlvec-fns "sql/discrete_tree.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-tree
  {:id                        nil
   :user-id                   nil
   :tree-file-url             nil
   :locations-file-url        nil
   :readable-name             nil
   :locations-attribute-name  nil
   :timescale-multiplier      nil
   :most-recent-sampling-date nil
   :output-file-url           nil})

(defn upsert! [db tree]
  (let [tree (merge nil-tree tree)]
    (log/debug "upsert!" tree)
    (upsert-tree db tree)))

(defn update! [db tree]
  (let [tree (merge nil-tree tree)]
    (log/debug "update!" tree)
    (update-tree db tree)))

(defn insert-attributes! [db tree-id attributes]
  (doseq [att attributes]
    (insert-attribute db {:tree-id tree-id :attribute-name att})))

(defn upsert-status! [db status]
  (let [status (->> status
                    (merge {:tree-id nil :status nil :progress nil})
                    (#(update % :status name)))]
    (log/debug "upsert-status!" status)
    (upsert-status db status)))
