(ns api.models.user
  (:require [hugsql.core :as hugsql]))

;; These are just not to upset clj-kondo
(declare upsert-user)
(declare get-user-by-id)
(declare get-user-by-email)
(declare count-user-analysis*)
(declare search-user-analysis*)

(hugsql/def-db-fns "sql/user.sql")
(hugsql/def-sqlvec-fns "sql/user.sql")

(defn search-user-analysis [db {:keys [statuses user-id] :as args}]
  (let [statuses (map name statuses)]
    ;; TODO : wrap in one transaction
    {:total-count (:total-count (count-user-analysis* db {:user-id user-id :statuses statuses}))
     :analysis    (search-user-analysis* db (assoc args :statuses statuses))}))
