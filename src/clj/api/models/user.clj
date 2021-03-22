(ns api.models.user
  (:require [hugsql.core :as hugsql]))

;; These are just not to upset clj-kondo
(declare upsert-user)
(declare get-user-by-id)
(declare get-user-by-email)
(declare search-user-analysis)

(hugsql/def-db-fns "sql/user.sql")
(hugsql/def-sqlvec-fns "sql/user.sql")
