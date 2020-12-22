(ns tests.integration.discrete-tree-test
  (:require
   [clj-http.client :as http]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :refer [use-fixtures deftest is]]
   [taoensso.timbre :as log]
   [tests.integration.utils :refer [run-query db-fixture]]
   ))

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
      (if (= status current-status)
        current-status
        (recur (query-status id))))))

(deftest discrete-tree-test
  (let [[tree-url locations-url] (get-in (run-query {:query
                                                     "mutation GetUploadUrls($files: [File]) {
                                                        getUploadUrls(files: $files)
                                                      }"
                                                     :variables {:files [{:name "H5N1_HA_discrete_MCC"
                                                                          :extension "tree"}
                                                                         {:name "locationCoordinates_H5N1"
                                                                          :extension "txt"}]}})
                                         [:data :getUploadUrls])
        _ (http/put tree-url {:body (io/file "src/test/resources/discrete/H5N1_HA_discrete_MCC.tree")})
        _ (http/put locations-url {:body (io/file "src/test/resources/discrete/locationCoordinates_H5N1")})
        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadTree($treeUrl: String!,
                                                                     $locationsUrl: String!) {
                                                   uploadDiscreteTree(treeFileUrl: $treeUrl,
                                                                      locationsFileUrl: $locationsUrl) {
                                                     id
                                                     status
                                                   }
                                                }"
                                                :variables {:treeUrl (-> tree-url
                                                                         (string/split  #"\?")
                                                                         first)
                                                            :locationsUrl (-> locations-url
                                                                              (string/split  #"\?")
                                                                              first)}})
                                    [:data :uploadDiscreteTree])

        _ (is :TREE_AND_LOCATIONS_UPLOADED (keyword status))

        _ (block-on-status id :ATTRIBUTES_PARSED)

        ]

    (log/debug "response" {
                           :tree/url tree-url
                           :locations/url locations-url
                           :tree/id id
                           :status status
                           })

    ;; (is false)

    ))
