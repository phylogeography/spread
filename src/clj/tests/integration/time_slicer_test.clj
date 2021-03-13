(ns tests.integration.time-slicer-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [use-fixtures deftest is]]
            [tests.integration.utils :refer [run-query db-fixture]]))

(use-fixtures :once db-fixture)

(defn- block-on-status [id status]
  (let [query-status #(-> (get-in (run-query {:query
                                              "query GetStatus($id: ID!) {
                                                 getTimeSlicer(id: $id) {
                                                   status
                                                 }
                                               }"
                                              :variables {:id %}})
                                  [:data :getTimeSlicer :status])
                          keyword)]
    (loop [current-status (query-status id)]
      (if (= status current-status)
        current-status
        (recur (query-status id))))))

(deftest time-slicer-test
  (let [[url _] (get-in (run-query {:query
                                    "mutation GetUploadUrl($files: [File]) {
                                       getUploadUrls(files: $files)
                                     }"
                                    :variables {:files [{:name      "WNV_small"
                                                         :extension "trees"}]}})
                        [:data :getUploadUrls])

        _ (http/put url {:body (io/file "src/test/resources/timeSlicer/WNV_small.trees")})

        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadTimeSlicer($url: String!) {
                                                   uploadTimeSlicer(treesFileUrl: $url) {
                                                     id
                                                     status
                                                   }
                                                }"
                                                :variables {:url (-> url
                                                                     (string/split  #"\?")
                                                                     first)}})
                                    [:data :uploadTimeSlicer])

        _ (is :TREES_UPLOADED (keyword status))

        _ (block-on-status id :ATTRIBUTES_AND_TREES_COUNT_PARSED)

        {:keys [id attributeNames treesCount]} (get-in (run-query {:query
                                                                   "query GetTree($id: ID!) {
                                                                            getTimeSlicer(id: $id) {
                                                                              id
                                                                              treesCount
                                                                              attributeNames
                                                                            }
                                                                          }"
                                                                   :variables {:id id}})
                                                       [:data :getTimeSlicer])

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateTree($id: ID!,
                                                                  $burnIn: Int!,
                                                                  $numberOfIntervals : Int!,
                                                                  $rrwRateAttributeName: String!,
                                                                  $traitAttributeName: String!,
                                                                  $contouringGridSize: Int!,
                                                                  $hpd: Float!,
                                                                  $mrsd: String!) {
                                                   updateTimeSlicer(id: $id,
                                                                    burnIn: $burnIn,
                                                                    numberOfIntervals: $numberOfIntervals,
                                                                    relaxedRandomWalkRateAttributeName: $rrwRateAttributeName,
                                                                    traitAttributeName: $traitAttributeName,
                                                                    contouringGridSize: $contouringGridSize,
                                                                    hpdLevel: $hpd,
                                                                    mostRecentSamplingDate: $mrsd) {
                                                     status
                                                   }
                                              }"
                                             :variables {:id                   id
                                                         :traitAttributeName   "location"
                                                         :rrwRateAttributeName "rate"
                                                         :contouringGridSize   100
                                                         :burnIn               1
                                                         :numberOfIntervals    10
                                                         :hpd                  0.8
                                                         :mrsd                 "2021/01/12"}})
                                 [:data :updateContinuousTree])

        _ (is :PARSER_ARGUMENTS_SET (keyword status))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startTimeSlicerParser(id: $id) {
                                                 status
                                                }
                                              }"
                                             :variables {:id id}})
                                 [:data :startTimeSlicerParser])

        _ (is :QUEUED (keyword status))

        _ (block-on-status id :SUCCEEDED)

        {:keys [outputFileUrl progress]} (get-in (run-query {:query
                                                             "query GetTree($id: ID!) {
                                                                            getTimeSlicer(id: $id) {
                                                                              id
                                                                              status
                                                                              progress
                                                                              outputFileUrl
                                                                            }
                                                                          }"
                                                             :variables {:id id}})
                                                 [:data :getTimeSlicer])]

    (is #{"rate" "location"} (set attributeNames))
    (is (= 10 treesCount))
    (is (= 1.0 progress))
    (is outputFileUrl)))
