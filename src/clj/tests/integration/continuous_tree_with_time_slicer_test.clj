(ns tests.integration.continuous-tree-with-time-slicer-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [shared.time :as time]
            [taoensso.timbre :as log]
            [tests.integration.continuous-tree-test :refer [block-on-status]]
            [tests.integration.utils :refer [db-fixture run-query]]))

(use-fixtures :once db-fixture)

(deftest continuous-tree-with-time-slicer-test
  (let [[trees-url mcc-tree-url] (get-in (run-query {:query
                                                     "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                     :variables {:files [{:name      "WNV_small"
                                                                          :extension "trees"}
                                                                         {:name      "WNV_MCC"
                                                                          :extension "tree"}]}})
                                         [:data :getUploadUrls])

        _ (http/put trees-url {:body (io/file "src/test/resources/timeSlicer/WNV_small.trees")})
        _ (http/put mcc-tree-url {:body (io/file "src/test/resources/timeSlicer/WNV_MCC.tre")})

        {continuous-tree-id :id status :status}
        (get-in (run-query {:query
                            "mutation UploadTree($url: String!, $name: String!) {
                                                   uploadContinuousTree(treeFileUrl: $url,
                                                                        readableName: $name) {
                                                     id
                                                     status
                                                   }
                                                }"
                            :variables {:name "WNV.tre"
                                        :url  (-> mcc-tree-url
                                                  (string/split  #"\?")
                                                  first)}})
                [:data :uploadContinuousTree])

        _ (is (= :UPLOADED (keyword status)))

        _ (block-on-status continuous-tree-id :ATTRIBUTES_PARSED)

        {:keys [attributeNames]} (get-in (run-query {:query
                                                     "query GetTree($id: ID!) {
                                                                            getContinuousTree(id: $id) {
                                                                              id
                                                                              status
                                                                              attributeNames
                                                                            }
                                                                          }"
                                                     :variables {:id continuous-tree-id}})
                                         [:data :getContinuousTree])

        {time-slicer-id :id status :status}
        (get-in (run-query {:query
                            "mutation UploadTimeSlicer($continuousTreeId: ID!, $url: String!) {
                                                   uploadTimeSlicer(continuousTreeId: $continuousTreeId,
                                                                    treesFileUrl: $url) {
                                                     id
                                                     status
                                                   }
                                                }"
                            :variables {:url              (-> trees-url
                                                              (string/split  #"\?")
                                                              first)
                                        :continuousTreeId continuous-tree-id}})
                [:data :uploadTimeSlicer])

        _ (is (= :UPLOADED (keyword status)))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateTree($id: ID!,
                                                                  $x: String!,
                                                                  $y: String!,
                                                                  $mrsd: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        xCoordinateAttributeName: $x,
                                                                        yCoordinateAttributeName: $y,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     status
                                                   }
                                              }"
                                             :variables {:id   continuous-tree-id
                                                         :x    "location2"
                                                         :y    "location1"
                                                         :mrsd "2018/02/12"}})
                                 [:data :updateContinuousTree])

        _ (is (= :ARGUMENTS_SET (keyword status)))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startContinuousTreeParser(id: $id) {
                                                  id
                                                  status
                                                }
                                              }"
                                             :variables {:id continuous-tree-id}})
                                 [:data :startContinuousTreeParser])

        _ (is (= :QUEUED (keyword status)))

        _ (block-on-status continuous-tree-id :SUCCEEDED)

        {:keys [timeSlicer readableName createdOn status progress outputFileUrl]}
        (get-in (run-query {:query
                            "query GetTree($id: ID!) {
                                     getContinuousTree(id: $id) {
                                       id
                                       readableName
                                       createdOn
                                       status
                                       progress
                                       outputFileUrl
                                       timeSlicer {
                                         id
                                         status
                                       }
                                     }
                                   }"
                            :variables {:id continuous-tree-id}})
                [:data :getContinuousTree])



        ]

    (log/debug "response" {:id         continuous-tree-id
                           :name       readableName
                           :created-on createdOn
                           :status     status
                           :timeSlicer timeSlicer
                           })

    ;; TODO: not returned, why?
    (is (= :SUCCEEDED (:status timeSlicer)))

    (is #{"height" "height_95%_HPD" "height_median" "height_range" "length" "length_95%_HPD" "length_median" "length_range" "location_95%HPD_modality" "location1" "location1_95%HPD_1" "location1_95%HPD_10" "location1_95%HPD_11" "location1_95%HPD_12" "location1_95%HPD_13" "location1_95%HPD_14" "location1_95%HPD_15" "location1_95%HPD_16" "location1_95%HPD_17" "location1_95%HPD_18" "location1_95%HPD_19" "location1_95%HPD_2" "location1_95%HPD_20" "location1_95%HPD_21" "location1_95%HPD_22" "location1_95%HPD_23" "location1_95%HPD_24" "location1_95%HPD_25" "location1_95%HPD_26" "location1_95%HPD_27" "location1_95%HPD_28" "location1_95%HPD_3" "location1_95%HPD_4" "location1_95%HPD_5" "location1_95%HPD_6" "location1_95%HPD_7" "location1_95%HPD_8" "location1_95%HPD_9" "location1_median" "location1_range" "location2" "location2_95%HPD_1" "location2_95%HPD_10" "location2_95%HPD_11" "location2_95%HPD_12" "location2_95%HPD_13" "location2_95%HPD_14" "location2_95%HPD_15" "location2_95%HPD_16" "location2_95%HPD_17" "location2_95%HPD_18" "location2_95%HPD_19" "location2_95%HPD_2" "location2_95%HPD_20" "location2_95%HPD_21" "location2_95%HPD_22" "location2_95%HPD_23" "location2_95%HPD_24" "location2_95%HPD_25" "location2_95%HPD_26" "location2_95%HPD_27" "location2_95%HPD_28" "location2_95%HPD_3" "location2_95%HPD_4" "location2_95%HPD_5" "location2_95%HPD_6" "location2_95%HPD_7" "location2_95%HPD_8" "location2_95%HPD_9" "location2_median" "location2_range" "posterior" "rate" "rate_95%_HPD" "rate_median" "rate_range"}
        (set attributeNames))

    (is (= :SUCCEEDED (keyword status)))

    (is (= (:dd (time/now))
           (:dd (time/from-millis createdOn))))

    (is (= 1.0 progress))

    (is outputFileUrl)

    ))
