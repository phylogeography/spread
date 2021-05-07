(ns tests.integration.bayes-factor-test
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
                                                 getBayesFactorAnalysis(id: $id) {
                                                   status
                                                   }
                                                 }"
                                              :variables {:id %}})
                                  [:data :getBayesFactorAnalysis :status])
                          keyword)]
    (loop [current-status (query-status id)]
      (if (= status current-status)
        current-status
        (do
          (Thread/sleep 1000)
          (recur (query-status id)))))))

(deftest bayes-factor-test
  (let [[log-url locations-url] (get-in (run-query {:query
                                                    "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                    :variables {:files [{:name      "H5N1_HA_discrete_rateMatrix"
                                                                         :extension "log"}
                                                                        {:name      "locationCoordinates_H5N1"
                                                                         :extension "txt"}]}})
                                        [:data :getUploadUrls])
        _                       (http/put log-url {:body (io/file "src/test/resources/bayesFactor/H5N1_HA_discrete_rateMatrix.log")})
        _                       (http/put locations-url {:body (io/file "src/test/resources/bayesFactor/locationCoordinates_H5N1")})

        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadBayesFactor($logUrl: String!,
                                                                            $locationsUrl: String!) {
                                                   uploadBayesFactorAnalysis(logFileUrl: $logUrl,
                                                                             locationsFileUrl: $locationsUrl) {
                                                     id
                                                     status
                                                  }
                                                }"
                                                :variables {:logUrl       (-> log-url
                                                                              (string/split  #"\?")
                                                                              first)
                                                            :locationsUrl (-> locations-url
                                                                              (string/split  #"\?")
                                                                              first)}})
                                    [:data :uploadBayesFactorAnalysis])

        _ (is :UPLOADED (keyword status))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation UpdateBayesFactor($id: ID!,
                                                                         $burnIn: Float!) {
                                                updateBayesFactorAnalysis(id: $id,
                                                                          burnIn: $burnIn) {
                                                  status
                                                }
                                              }"
                                             :variables {:id     id
                                                         :burnIn 0.1}})
                                 [:data :updateBayesFactorAnalysis])

        _ (is :ARGUMENTS_SET (keyword status))

        {:keys [status]} (get-in (run-query {:query
                                             "mutation QueueJob($id: ID!) {
                                                startBayesFactorParser(id: $id) {
                                                 status
                                                }
                                              }"
                                             :variables {:id id}})
                                 [:data :startBayesFactorParser])

        _ (is :QUEUED (keyword status))

        _ (block-on-status id :SUCCEEDED)

        {:keys [id readableName createdOn status progress outputFileUrl bayesFactors]}
        (get-in (run-query {:query
                            "query GetResults($id: ID!) {
                                     getBayesFactorAnalysis(id: $id) {
                                       id
                                       readableName
                                       createdOn
                                       status
                                       progress
                                       outputFileUrl
                                           bayesFactors {
                                             from
                                             to
                                             bayesFactor
                                             posteriorProbability
                                           }
                                     }
                                   }"
                            :variables {:id id}})
                [:data :getBayesFactorAnalysis])]

    (log/debug "response" {:id            id
                           :name          readableName
                           :created-on    createdOn
                           :status        status
                           :bayes-factors bayesFactors})

    (is (= (:dd (time/now))
           (:dd (time/from-millis createdOn))))

    (is (sequential? bayesFactors))

    (is (= 21 (count bayesFactors)))

    (is #{"from" "to" "bayesFactors" "posteriorProbability"} (-> bayesFactors first keys set))

    (is (= 1.0 progress))

    (is outputFileUrl)))
