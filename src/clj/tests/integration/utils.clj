(ns tests.integration.utils
  (:require
   [api.config :as config]
   [api.db :as db]
   [api.models.user :as user-model]
   [clj-http.client :as http]
   [clojure.data.json :as json]
   ))

(defn run-query [{:keys [url query variables]
                  :or {url "http://localhost:3001/api"}}]
  (let [{:keys [body]} (http/post url {:form-params {:query query
                                                     :variables variables}
                                       :content-type "application/json"})]
    (json/read-str body :key-fn keyword)))

(defn db-fixture [f]
  (let [config (config/load!)
        db (db/init (:db config))]
    ;; TODO : randomize user once we have auth
    (user-model/upsert-user db {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
                                :email "test@test.com"})
    (f)))
