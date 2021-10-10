(ns api.models.bayes-factor
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-bayes-factor-analysis)
(declare get-bayes-factor-analysis)
(declare insert-bayes-factors)
(declare get-bayes-factors)

(hugsql/def-db-fns "sql/bayes_factor.sql")
(hugsql/def-sqlvec-fns "sql/bayes_factor.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-bayes-factor-analysis
  {:id                  nil
   :log-file-url        nil
   :log-file-name       nil
   :locations-file-url  nil
   :locations-file-name nil
   :number-of-locations nil
   :burn-in             nil
   :output-file-url     nil})

(defn upsert! [db {:keys [id] :as analysis}]
  (let [prev     (or (get-bayes-factor-analysis db {:id id})
                     nil-bayes-factor-analysis)
        analysis (merge prev analysis)]
    (log/debug "upsert!" analysis)
    (upsert-bayes-factor-analysis db analysis)))
