(ns ui.events.analysis-results
  (:require [clojure.string :as str]
            [ui.file-fxs]
            [ui.s3 :as s3]
            [ui.utils :as ui-utils :refer [>evt]]))

(defn initialize-page
  [_]
  ;; NOTE : dispatch query when all results loaded
  ;; loading is triggered by general/initialize which dispatches a graphql query
  {:async-flow {:first-dispatch [:analysis-results/initial-query]
                ;; NOTE if no data in app db try again after it's loaded
                :rules          [{:when     :seen?
                                  :events   [:user-analysis-loaded]
                                  :dispatch [:analysis-results/initial-query]}]}})

(defn initial-query
  [{:keys [db]}]
  (let [id                (-> db :ui.router :active-page :query :id)
        {:keys [of-type]} (get-in db [:analysis id])]
    (when of-type
      {:dispatch [:general/query-analysis {:id id :of-type of-type}]})))

(defn export-bayes-table-to-csv [_ [_ bayes-factors]]
  (let [first-row (first bayes-factors)
        headers (keys first-row)
        headers-row (str/join "," (map name headers))
        data (->> bayes-factors
                  (map (fn [row]
                         (->> headers
                              (map #(get row %))
                              (str/join ","))))
                  (str/join "\n"))
        file-content (str headers-row "\n" data )]
    {:spread/download-text-as-file {:text file-content
                                    :file-name "bayes-data.csv"
                                    :file-type "data:text/csv;charset=utf-8"}}))

(defn on-custom-map-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (str/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "json"}
                                :on-success [:analysis-results/upload-custom-map-file file-with-meta]}]}))

(defn upload-custom-map-file [_ [_ {:keys [data filename analysis-id]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:analysis-results/custom-map-file-success {:url url
                                                                                      :analysis-id analysis-id
                                                                                      :filename filename}])

                  :on-error        #(prn "ERROR" %)
                  ;; we don't handle progress since custom map are very small files
                  :handle-progress (fn [_ _])}}))

(defn custom-map-file-success [_ [_ {:keys [url filename analysis-id]}]]
  (let [[url _]       (str/split url "?")]
    {:dispatch [:graphql/query {:query
                                "mutation UploadCustomMap($analysisId: ID! $fileUrl: String!, $fileName: String!) {
                                              uploadCustomMap(analysisId: $analysisId, fileUrl: $fileUrl, fileName: $fileName) {
                                                analysisId,
                                                fileName,
                                                fileUrl
                                                }
                                              }"
                                :variables {:analysisId analysis-id
                                            :fileName filename
                                            :fileUrl  url}}]}))

(defn delete-custom-map [_ [_ analysis-id]]
  {:dispatch [:graphql/query {:query
                              "mutation DeleteCustomMap($analysisId: ID!) {
                                              deleteCustomMap(analysisId: $analysisId)
                                              }"
                              :variables {:analysisId analysis-id}}]})
