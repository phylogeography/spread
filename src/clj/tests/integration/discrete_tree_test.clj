(ns tests.integration.discrete-tree-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures]]
            [shared.time :as time]
            [taoensso.timbre :as log]
            [tests.integration.utils :refer [db-fixture run-query]]))

(use-fixtures :once db-fixture)

(defn- block-on-status [id status]
  (let [query-status #(-> (get-in (run-query {:query
                                              "query GetStatus($id: ID!) {
                                                 getDiscreteTree(id: $id) {
                                                   status
                                                   }
                                                 }"
                                              :variables {:id %}})
                                  [:data :getDiscreteTree :status])
                          keyword)]
    (loop [current-status (query-status id)]
      (if (or (= status current-status)
              (= :ERROR current-status))
        current-status
        (do
          (Thread/sleep 1000)
          (recur (query-status id)))))))

(deftest discrete-tree-test
  (let [[tree-url locations-url] (get-in (run-query {:query
                                                     "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                     :variables {:files [{:name      "H5N1_HA_discrete_MCC"
                                                                          :extension "tree"}
                                                                         {:name      "locationCoordinates_H5N1"
                                                                          :extension "txt"}]}})
                                         [:data :getUploadUrls])
        _                        (http/put tree-url {:body (io/file "src/test/resources/discrete/H5N1_HA_discrete_MCC.tree")})
        _                        (http/put locations-url {:body (io/file "src/test/resources/discrete/locationCoordinates_H5N1")})

        ;; TODO : asjust to fit new API
        {:keys [id status]}      (get-in (run-query {:query
                                                     "mutation UploadTree($treeUrl: String!,
                                                                          $locationsUrl: String!,
                                                                          $name: String!) {
                                                        uploadDiscreteTree(readableName: $name,
                                                                           treeFileUrl: $treeUrl,
                                                                           locationsFileUrl: $locationsUrl) {
                                                          id
                                                          status
                                                        }
                                                      }"
                                                     :variables {:name         "H5N1_HA_discrete_MCC"
                                                                 :treeUrl      (-> tree-url
                                                                                   (string/split  #"\?")
                                                                                   first)
                                                                 :locationsUrl (-> locations-url
                                                                                   (string/split  #"\?")
                                                                                   first)}})
                                         [:data :uploadDiscreteTree])

        _ (is (= :UPLOADED (keyword status)))

        _ (block-on-status id :ATTRIBUTES_PARSED)

        {:keys [attributeNames]} (get-in (run-query {:query
                                                     "query GetTree($id: ID!) {
                                                                            getDiscreteTree(id: $id) {
                                                                              attributeNames
                                                                            }
                                                                          }"
                                                     :variables {:id id}})
                                         [:data :getDiscreteTree])

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateTree($id: ID!,
                                                                  $locationsAttribute: String!,
                                                                  $mrsd: String!) {
                                                updateDiscreteTree(id: $id,
                                                                   locationsAttributeName: $locationsAttribute,
                                                                   mostRecentSamplingDate: $mrsd) {
                                                  status
                                                }
                                              }"
                                             :variables {:id                 id
                                                         :locationsAttribute "states"
                                                         :mrsd               "2019/02/12"}})
                                 [:data :updateDiscreteTree])

        _ (is (= :ARGUMENTS_SET (keyword status)))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startDiscreteTreeParser(id: $id) {
                                                 status
                                                }
                                              }"
                                             :variables {:id id}})
                                 [:data :startDiscreteTreeParser])

        _ (is (= :QUEUED (keyword status)))

        _ (block-on-status id :SUCCEEDED)

        {:keys [id readableName createdOn status progress outputFileUrl]}
        (get-in (run-query {:query
                            "query GetTree($id: ID!) {
                                     getDiscreteTree(id: $id) {
                                       id
                                       readableName
                                       createdOn
                                       status
                                       progress
                                       outputFileUrl
                                     }
                                   }"
                            :variables {:id id}})
                [:data :getDiscreteTree])]

    (log/debug "response" {:id            id
                           :name          readableName
                           :created-on    createdOn
                           :status        status
                           :progress      progress
                           :tree/url      tree-url
                           :locations/url locations-url})

    (is (= (:dd (time/now))
           (:dd (time/from-millis createdOn))))

    (is #{"height" "height_95%_HPD" "height_median" "height_range" "length" "length_95%_HPD"
          "length_median" "length_range" "posterior" "rate" "rate_95%_HPD" "rate_median"
          "rate_range" "states" "states.prob" "states.set" "states.set.prob"}
        (set attributeNames))

    (is (= 1.0 progress))

    (is outputFileUrl)))
