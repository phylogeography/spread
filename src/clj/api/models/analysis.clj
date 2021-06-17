(ns api.models.analysis
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-analysis)
;; (declare upsert-status)

(hugsql/def-db-fns "sql/analysis.sql")
(hugsql/def-sqlvec-fns "sql/analysis.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-entity
  {:id            nil
   :user-id       nil
   :readable-name nil
   :created-on    nil
   :status        nil
   :of-type       nil})

(defn upsert! [db entity]
  (let [entity (-> (merge nil-entity entity)
                   (update % :status name))]
    (log/debug "upsert-analysis" entity)
    (upsert-analysis db entity)))
