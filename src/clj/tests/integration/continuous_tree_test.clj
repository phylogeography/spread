(ns tests.integration.continuous-tree-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [shared.time :as time]
            [taoensso.timbre :as log]
            [tests.integration.utils :refer [db-fixture run-query]]))

(use-fixtures :once db-fixture)

(defn block-on-status [id status]
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
      (if (or (= status current-status)
              (= :ERROR current-status))
        current-status
        (do
          (Thread/sleep 1000)
          (recur (query-status id)))))))

(deftest continuous-tree-test
  (let [[url _] (get-in (run-query {:query
                                    "mutation GetUploadUrl($files: [File]) {
                                       getUploadUrls(files: $files)
                                     }"
                                    :variables {:files [{:name      "speciesDiffusion.MCC"
                                                         :extension "tree"}]}})
                        [:data :getUploadUrls])

        filename "speciesDiffusion.MCC.tre"

        _ (http/put url {:body (io/file (str "src/test/resources/continuous/" filename))})

        url (-> url (string/split  #"\?") first)

        {:keys [id status treeFileName]} (get-in (run-query
                                                   {:query
                                                    "mutation UploadContinuousTreeDiscreteTree($treeFileUrl: String!, $treeFileName: String!) {
                                              uploadContinuousTree(treeFileUrl: $treeFileUrl, treeFileName: $treeFileName) {
                                                id
                                                status
                                                readableName
                                                treeFileName
                                                createdOn
                                                }
                                              }"
                                                    :variables {:treeFileUrl  url
                                                                :treeFileName filename}})
                                                 [:data :uploadContinuousTree])

        _ (is (= :UPLOADED (keyword status)))
        _ (is (= filename treeFileName))

        _ (block-on-status id :ATTRIBUTES_PARSED)

        {:keys [id attributeNames]} (get-in (run-query {:query
                                                        "query GetTree($id: ID!) {
                                                           getContinuousTree(id: $id) {
                                                             id
                                                             status
                                                             attributeNames
                                                           }
                                                         }"
                                                        :variables {:id id}})
                                            [:data :getContinuousTree])

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateTree($id: ID!,
                                                                  $x: String!,
                                                                  $y: String!,
                                                                  $mrsd: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        xCoordinateAttributeName: $x,
                                                                        yCoordinateAttributeName: $y,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     id
                                                     status
                                                   }
                                              }"
                                             :variables {:id   id
                                                         :x    "trait2"
                                                         :y    "trait1"
                                                         :mrsd "2019/02/12"}})
                                 [:data :updateContinuousTree])

        _ (is (= :ARGUMENTS_SET (keyword status)))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startContinuousTreeParser(id: $id) {
                                                  status
                                                }
                                              }"
                                             :variables {:id id}})
                                 [:data :startContinuousTreeParser])

        _ (is (= :QUEUED (keyword status)))

        _ (block-on-status id :SUCCEEDED)

        {:keys [id readableName createdOn status progress outputFileUrl]}
        (get-in (run-query {:query
                            "query GetTree($id: ID!) {
                                     getContinuousTree(id: $id) {
                                       id
                                       readableName
                                       createdOn
                                       status
                                       progress
                                       outputFileUrl
                                     }
                                   }"
                            :variables {:id id}})
                [:data :getContinuousTree])

        ]

    (log/debug "response" {:id              id
                           :name            readableName
                           :created-on      createdOn
                           :status          status
                           :progress        progress
                           :output-file-url outputFileUrl})

    (is (= (:dd (time/now))
           (:dd (time/from-millis createdOn))))

    (is (= "speciesDiffusion" readableName))

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

    (is (= 1.0 progress))

    (is outputFileUrl)

    ))
