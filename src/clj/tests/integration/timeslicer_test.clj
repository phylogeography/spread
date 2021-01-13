(ns tests.integration.timeslicer-test
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [use-fixtures deftest is]]
            [taoensso.timbre :as log]
            [tests.integration.utils :refer [run-query db-fixture]]))

(use-fixtures :once db-fixture)

(deftest timeslicer-test
  (let [[url _] (get-in (run-query {:query
                                    "mutation GetUploadUrl($files: [File]) {
                                       getUploadUrls(files: $files)
                                     }"
                                    :variables {:files [{:name "WNV_small"
                                                         :extension "trees"}]}})
                        [:data :getUploadUrls])

        _ (http/put url {:body (io/file "src/test/resources/timeSlicer/WNV_small.trees")})

        {:keys [id status]} (get-in (run-query {:query
                                                "mutation UploadTimeslicer($url: String!) {
                                                   uploadTimeslicer(treesFileUrl: $url) {
                                                     id
                                                     status
                                                   }
                                                }"
                                                :variables {:url (-> url
                                                                     (string/split  #"\?")
                                                                     first)}})
                                    [:data :uploadTimeslicer])

        ]

    (log/debug "url" {:id id
                      :status status
                      ;; :url url
                      })

    (is false)

    ))
