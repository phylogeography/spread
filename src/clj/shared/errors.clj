(ns shared.errors
  (:require [api.auth :as auth]
            [api.models.analysis :as analysis-model]
            [api.models.error :as error-model]))

(defn handle-analysis-error! [db id error]
  (analysis-model/upsert! db {:id     id
                              :status :ERROR})
  (error-model/insert! db {:id    id
                           ;; TODO : do sth for human consumable errors
                           :error (str error)}))
