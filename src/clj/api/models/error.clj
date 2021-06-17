(ns api.models.error
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare insert-error)

(hugsql/def-db-fns "sql/error.sql")
(hugsql/def-sqlvec-fns "sql/error.sql")

(defn insert! [db entity]
  (log/debug "insert error" entity)
  (insert-error db entity))
