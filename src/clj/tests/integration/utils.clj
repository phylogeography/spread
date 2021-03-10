(ns tests.integration.utils
  (:require
   [api.config :as config]
   [api.db :as db]
   [api.models.user :as user-model]
   [clj-http.client :as http]
   [shared.utils :refer [decode-json]]))

;; this token will last 100 years :)
(def access-token "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzcHJlYWQiLCJpYXQiOjEuNjE1Mjk2NzI1MzY5RTksImV4cCI6NC4yNDMyOTY3MjUzNjlFOSwiYXVkIjoic3ByZWFkLWNsaWVudCIsInN1YiI6ImExMTk1ODc0LTBiYmUtNGE4Yy05NmY1LTE0Y2RmOTA5N2UwMiJ9.ZdT-j8BJStTC4FZFawZPoZBXlHJ1AQc2A9T3xxzQYUdBntyCtxUPuKGBNyHLdJmfzdUm66LgVlZw1kiyXbh4xw")

(defn run-query [{:keys [url query variables]
                  :or   {url "http://localhost:3001/api"}}]
  (let [{:keys [body]} (http/post url {:form-params  {:query     query
                                                      :variables variables}
                                       :headers      {"Authorization" (str "Bearer " access-token)}
                                       :content-type "application/json"})]
    (decode-json body)))

(defn db-fixture [f]
  (let [config (config/load!)
        db     (db/init (:db config))]
    ;; TODO : randomize user once we have auth
    (user-model/upsert-user db {:id    "a1195874-0bbe-4a8c-96f5-14cdf9097e02"
                                :email "test@test.com"})
    (f)))
