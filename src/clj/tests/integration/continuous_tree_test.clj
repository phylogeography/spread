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

(defn run-query [{:keys [url query variables]
                  :or {url "http://localhost:3001/api"}}]
  (let [{:keys [body] :as response} (http/post url {:form-params {:query query
                                                                  :variables variables}
                                                    :content-type "application/json"})]
    (json/read-str body :key-fn keyword)))

(defn- block-on-status [id status]
  (let [query-status #(-> (get-in (run-query {:query
                                              "query GetStatus($id: ID!) {
                                                   getContinuousTree(id: $id) {
                                                     status
                                                   }
                                                }"
                                              :variables {:id %}})
                                  [:data :getContinuousTree :status])
                          keyword)]
    (loop [current-status (query-status id)]
      (if (= status current-status)
        current-status
        (recur (query-status id))))))

(use-fixtures :once (fn [f]
                      (let [config (config/load)
                            db (db/init (:db config))]
                        ;; TODO : randomize user once we have auth
                        (user-model/upsert-user db {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
                                                    :email "test@test.com"})
                        (f))))


(deftest continuous-tree-test
  (let [

        ;; TODO : query atts and hpd-levels
        ;; TODO : set all settings
        ;; TODO : parse
        ;; TODO : run assertions

        [url _] (get-in (run-query {:query
                                    "mutation GetUploadUrl($files: [File]) {
                                       getUploadUrls(files: $files)
                                     }"
                                    :variables {:files [{:name "speciesDiffusion.MCC"
                                                         :extension "tree"}]}})
                        [:data :getUploadUrls])

        file (clojure.java.io/file "src/test/resources/continuous/speciesDiffusion.MCC.tre")
        _ (http/put url {:body file})

        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadTree($url: String!) {
                                                   uploadContinuousTree(treeFileUrl: $url) {
                                                     id
                                                     status
                                                   }
                                                }"
                                                :variables {:url (-> url
                                     (string/split  #"\?")
                                     first)}})
                                    [:data :uploadContinuousTree])

        _ (is :TREE_UPLOADED (keyword status))

        ;; TODO : parsed?
        _ (block-on-status id :ATTRIBUTES_AND_HPD_LEVELS_PARSED)

        {:keys [id status] :as resp} (get-in (run-query {:query
                                                "query GetTree($id: ID!) {
                                                   getContinuousTree(id: $id) {
                                                     id
                                                     status
                                                     hpdLevels
                                                     attributeNames
                                                   }
                                                }"
                                                :variables {:id id}})
                                    [:data #_:getContinuousTree])


        ]

    (log/debug "response" {:url url
                           :r resp
                           ;; :status status
                           ;; :id id
                           })

    (is false)

    ))
