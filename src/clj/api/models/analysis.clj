(ns api.models.analysis
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-analysis)
(declare get-analysis)

(hugsql/def-db-fns "sql/analysis.sql")
(hugsql/def-sqlvec-fns "sql/analysis.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-analysis
  {:id            nil
   :user-id       nil
   :readable-name nil
   :created-on    nil
   :status        nil
   :of-type       nil})

(defn upsert! [db analysis]
  (let [analysis (-> (merge nil-analysis analysis)
                     (update :status name))]
    (log/debug "upsert-analysis" analysis)
    (upsert-analysis db analysis)))
