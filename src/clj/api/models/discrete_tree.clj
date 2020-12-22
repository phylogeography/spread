(ns api.models.discrete-tree
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-tree)


(hugsql/def-db-fns "sql/discrete_tree.sql")
(hugsql/def-sqlvec-fns "sql/discrete_tree.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-tree
  {:id nil
   :user-id nil
   :tree-file-url nil
   :locations-file-url nil
   :status nil
   :readable-name nil
   :locations-attribute-name nil
   :timescale-multiplier nil
   :most-recent-sampling-date nil
   :output-file-url nil
   })

(defn upsert-tree! [db tree]
  (let [tree (->> tree
                  (merge nil-tree)
                  (#(update % :status name)))]
    (log/debug "upsert-tree!" tree)
    (upsert-tree db tree)))
