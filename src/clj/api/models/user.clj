(ns api.models.user
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-user)

(hugsql/def-db-fns "sql/user.sql")
(hugsql/def-sqlvec-fns "sql/user.sql")