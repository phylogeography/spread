(ns tests.integration.continuous-tree-test
  (:require [api.config :as config]
            [api.db :as db]
            [api.models.user :as user-model]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [use-fixtures deftest is]]
            [taoensso.timbre :as log]))

(defn run-query [{:keys [url query variables]
                  :or {url "http://localhost:3001/api"}}]
  (let [{:keys [body]} (http/post url {:form-params {:query query
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
                      (let [config (config/load!)
                            db (db/init (:db config))]
                        ;; TODO : randomize user once we have auth
                        (user-model/upsert-user db {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
                                                    :email "test@test.com"})
                        (f))))

(deftest continuous-tree-test
  (let [[url _] (get-in (run-query {:query
                                    "mutation GetUploadUrl($files: [File]) {
                                       getUploadUrls(files: $files)
                                     }"
                                    :variables {:files [{:name "speciesDiffusion.MCC"
                                                         :extension "tree"}]}})
                        [:data :getUploadUrls])

        _ (http/put url {:body (io/file "src/test/resources/continuous/speciesDiffusion.MCC.tre")})

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

        _ (block-on-status id :ATTRIBUTES_AND_HPD_LEVELS_PARSED)

        {:keys [id attributeNames hpdLevels]} (get-in (run-query {:query
                                                                  "query GetTree($id: ID!) {
                                                                            getContinuousTree(id: $id) {
                                                                              id
                                                                              status
                                                                              hpdLevels
                                                                              attributeNames
                                                                            }
                                                                          }"
                                                                  :variables {:id id}})
                                                      [:data :getContinuousTree])

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateTree($id: ID!,
                                                                  $x: String!,
                                                                  $y: String!,
                                                                  $hpd: String!,
                                                                  $mrsd: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        xCoordinateAttributeName: $x,
                                                                        yCoordinateAttributeName: $y,
                                                                        hpdLevel: $hpd,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     status
                                                   }
                                              }"
                                             :variables {:id id
                                                         :x "trait2"
                                                         :y "trait1"
                                                         :hpd "80"
                                                         :mrsd "2019/02/12"}})
                                 [:data :updateContinuousTree])

        _ (is :PARSER_ARGUMENTS_SET (keyword status))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startContinuousTreeParser(id: $id) {
                                                 status
                                                }
                                              }"
                                             :variables {:id id}})
                                 [:data :startContinuousTreeParser])

        _ (is :QUEUED (keyword status))

        _ (block-on-status id :SUCCEEDED)

        {:keys [id status outputFileUrl]} (get-in (run-query {:query
                                                              "query GetTree($id: ID!) {
                                                                            getContinuousTree(id: $id) {
                                                                              id
                                                                              status
                                                                              outputFileUrl
                                                                            }
                                                                          }"
                                                              :variables {:id id}})
                                                  [:data :getContinuousTree])]

    (log/debug "response" {:id id :status status})

    (is #{"height" "height_95%_HPD" "height_median" "height_range"
          "length" "length_95%_HPD" "length_median" "length_range"
          "posterior" "trait_80%HPD_modality" "trait.rate" "trait.rate_95%_HPD"
          "trait.rate_median" "trait.rate_range" "trait1" "trait1_80%HPD_1"
          "trait1_80%HPD_2" "trait1_80%HPD_3" "trait1_80%HPD_4" "trait1_80%HPD_5"
          "trait1_80%HPD_6" "trait1_80%HPD_7" "trait1_80%HPD_8"
          "trait1_80%HPD_9" "trait1_median" "trait1_range"
          "trait2" "trait2_80%HPD_1" "trait2_80%HPD_2"
          "trait2_80%HPD_3" "trait2_80%HPD_4" "trait2_80%HPD_5"
          "trait2_80%HPD_6" "trait2_80%HPD_7" "trait2_80%HPD_8"
          "trait2_80%HPD_9" "trait2_median" "trait2_range"}
        (set attributeNames))

    (is #{"80"} (set hpdLevels))

    (is outputFileUrl)))
