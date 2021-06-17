(ns shared.errors
  (:require [api.auth :as auth]
            [api.models.analysis :as analysis-model]
            [api.models.error :as error-model]))

(defn handle-analysis-error [id error]
  ;; TODO : in a transaction
  (analysis-model/upsert! db {:id     id
                              :status :ERROR})
  (error-model/insert! {:id    id
                        ;; TODO : do sth for human consumable errors
                        :error (str error)}))
