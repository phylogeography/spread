(ns api.models.bayes-factor
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-bayes-factor-analysis)
(declare update-bayes-factor-analysis)
(declare get-bayes-factor-analysis)
(declare delete-bayes-factor-analysis)
;; TODO : get-bayes-factors

(hugsql/def-db-fns "sql/bayes_factor.sql")
(hugsql/def-sqlvec-fns "sql/bayes_factor.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-bayes-factor-analysis
  {:id nil
   :user-id nil
   :log-file-url nil
   :locations-file-url nil
   :status nil
   :readable-name nil
   :burn-in nil
   :output-file-url nil
   })

#_(defn upsert! [db tree]
  (let [tree (->> tree
                  (merge nil-tree)
                  (#(update % :status name)))]
    (log/debug "upsert-tree!" tree)
    (upsert-tree db tree)))

#_(defn update! [db tree]
  (let [tree (->> tree
                  (merge nil-tree)
                  (#(update % :status name)))]
    (log/debug "update-tree!" tree)
    (update-tree db tree)))
