(ns tests.integration.bayes-factor-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [use-fixtures deftest is]]
            [taoensso.timbre :as log]
            [tests.integration.utils :refer [run-query db-fixture]]))

(use-fixtures :once db-fixture)

(deftest continuous-tree-test
  (let [

        [tree-url locations-url] (get-in (run-query {:query
                                                     "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                     :variables {:files [{:name      "H5N1_HA_discrete_rateMatrix"
                                                                          :extension "log"}
                                                                         {:name      "locationCoordinates_H5N1"
                                                                          :extension "txt"}]}})
                                         [:data :getUploadUrls])
        _ (http/put tree-url {:body (io/file "src/test/resources/bayesFactor/H5N1_HA_discrete_rateMatrix.log")})
        _ (http/put locations-url {:body (io/file "src/test/resources/bayesFactor/locationCoordinates_H5N1")})

        #_#_{:keys [id status]} (get-in (run-query {:query
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

]

    #_(log/debug "response" {:id id :status status})



    (is true)

    ))
