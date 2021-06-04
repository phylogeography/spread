(ns api.models.parser
  (:require [hugsql.core :as hugsql]))

;; These are just not to upset clj-kondo
(declare get-status)

(hugsql/def-db-fns "sql/parser.sql")
(hugsql/def-sqlvec-fns "sql/parser.sql")
