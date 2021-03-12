(ns api.models.continuous-tree
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-tree)
(declare update-tree)
(declare insert-attribute)
(declare insert-hpd-level)
(declare get-tree)
(declare delete-tree)
(declare get-attributes)
(declare get-hpd-levels)
(declare get-status)

(hugsql/def-db-fns "sql/continuous_tree.sql")
(hugsql/def-sqlvec-fns "sql/continuous_tree.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-tree
  {:id nil
   :user-id nil
   :tree-file-url nil
   :status nil
   :progress nil
   :readable-name nil
   :x-coordinate-attribute-name nil
   :y-coordinate-attribute-name nil
   :hpd-level nil
   :has-external-annotations nil
   :timescale-multiplier nil
   :most-recent-sampling-date nil
   :output-file-url nil})

(defn upsert! [db tree]
  (let [tree (->> tree
                  (merge nil-tree)
                  (#(update % :status name)))]
    (log/debug "upsert-tree!" tree)
    (upsert-tree db tree)))

(defn update! [db tree]
  (let [tree (->> tree
                  (merge nil-tree)
                  (#(update % :status name)))]
    (log/debug "update-tree!" tree)
    (update-tree db tree)))

(defn insert-attributes! [db tree-id attributes]
  (doseq [att attributes]
    (insert-attribute db {:tree-id tree-id :attribute-name att})))

(defn insert-hpd-levels! [db tree-id levels]
  (doseq [lvl levels ]
    (insert-hpd-level db {:tree-id tree-id :level lvl})))
