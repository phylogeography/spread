(ns api.models.bayes-factor
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-bayes-factor-analysis)
(declare update-bayes-factor-analysis)
(declare get-bayes-factor-analysis)
(declare delete-bayes-factor-analysis)
(declare insert-bayes-factors)
(declare get-bayes-factors)
(declare get-status)

(hugsql/def-db-fns "sql/bayes_factor.sql")
(hugsql/def-sqlvec-fns "sql/bayes_factor.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-bayes-factor-analysis
  {:id                  nil
   :user-id             nil
   :log-file-url        nil
   :locations-file-url  nil
   :number-of-locations nil
   :status              nil
   :progress            nil
   :readable-name       nil
   :burn-in             nil
   :output-file-url     nil})

(defn upsert! [db analysis]
  (let [analysis (->> analysis
                      (merge nil-bayes-factor-analysis)
                      (#(update % :status name)))]
    (log/debug "upsert!" analysis)
    (upsert-bayes-factor-analysis db analysis)))

(defn update! [db analysis]
  (let [analysis (->> analysis
                      (merge nil-bayes-factor-analysis)
                      (#(update % :status name)))]
    (log/debug "update!" analysis)
    (update-bayes-factor-analysis db analysis)))
