(ns tests.integration.continuous-tree-test
  (:require [clojure.test :refer [use-fixtures deftest is testing]]
            [api.db :as db]
            [clj-http.client :as http]
            [clojure.string :as string]
            [shared.utils :refer [new-uuid]]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [api.config :as config]
            [api.models.user :as user-model]))

(defn run-query [{:keys [url query variables]}]
  (let [{:keys [body] :as response} (http/post url {:form-params {:query query
                                                 :variables variables}
                                   :content-type "application/json"})]
    (json/read-str body :key-fn keyword)))

(use-fixtures :once (fn [f]
                      (let [config (config/load)
                            db (db/init (:db config))
                            [first-name _] (shuffle ["Filip" "Philippe" "Guy" "Juan" "Kasper"])
                            [second-name _] (shuffle ["Fu" "Bar" "Smith" "Doe" "Hoe"])
                            [extension _] (shuffle ["io" "com" "gov"])]
                        #_(user-model/upsert-user db {:id (new-uuid)
                                                    :email (string/lower-case (str first-name "@" second-name "." extension))})
                        (f))))

(deftest continuous-tree-test
  (let [

        ;; TODO : upload tree
        ;; TODO : query atts and hpd-levels
        ;; TODO : set all settings
        ;; TODO : parse
        ;; TODO : run assertions

        resp (run-query {:url "http://localhost:3001/api"
                         :query
                         "query GetContinuousTree($id: ID!) {
                           getContinuousTree(id: $id) {
                             id
                           }
                         }"
                         :variables {:id "3eef35e9-f554-4032-89d3-deb347acd118"}})]

    (log/debug "response" resp)

    (is false)

    ))
