(ns ui.events.discrete-mcc-tree
  (:require [clojure.string :as string]
            [ui.s3 :as s3]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

(defn on-tree-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "tree"}
                                :on-success [:discrete-mcc-tree/s3-tree-file-upload file-with-meta]}]}))

(defn s3-tree-file-upload [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:discrete-mcc-tree/tree-file-upload-success {:url      url
                                                                                        :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:discrete-mcc-tree/tree-file-upload-progress (/ sent total)]))}}))

(defn tree-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :discrete-mcc-tree :tree-file-upload-progress] progress)})

(defn tree-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _]       (string/split url "?")
        readable-name (first (string/split filename "."))]
    {:dispatch [:graphql/query {:query     "mutation UploadDiscreteTree($treeFileUrl: String!) {
                                                uploadDiscreteTree(treeFileUrl: $treeFileUrl) {
                                                  id
                                                  status
                                                }
                                              }"
                                :variables {:treeFileUrl url}}]
     :db       (-> db
                   (assoc-in [:new-analysis :discrete-mcc-tree :tree-file] filename)
                   ;; default name: file name root
                   (assoc-in [:new-analysis :discrete-mcc-tree :readable-name] readable-name))}))

(defn delete-tree-file [{:keys [db]}]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                   deleteAnalysis(id: $analysisId) {
                                          id
                                        }
                                     }"
                                :variables {:analysisId id}}]
     :db       (dissoc-in db [:new-analysis :discrete-mcc-tree])}))

(defn delete-locations-file [{:keys [db]}]
  ;; NOTE : we just delete the object from S3 and dissoc the app-db values
  ;; there is no need to change the analysis settings in the DB as they are not set yet
  ;; this happens only when the analysis is started
  (let [{:keys [locations-file-url]} (get-in db [:new-analysis :discrete-mcc-tree])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteFile($url: String!) {
                                     deleteFile(url: $url) {
                                       key
                                     }
                                   }"
                                :variables {:url locations-file-url}}]
     :db       (-> db
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file])
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file-url])
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file-upload-progress]))}))

(defn on-locations-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "txt"}
                                :on-success [:discrete-mcc-tree/s3-locations-file-upload file-with-meta]}]}))

(defn s3-locations-file-upload [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:discrete-mcc-tree/locations-file-upload-success {:url      url
                                                                                             :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:discrete-mcc-tree/locations-file-upload-progress (/ sent total)]))}}))

(defn locations-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :discrete-mcc-tree :locations-file-upload-progress] progress)})

(defn locations-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _] (string/split url "?")]
    {:db (-> db
             (assoc-in [:new-analysis :discrete-mcc-tree :locations-file-url] url)
             (assoc-in [:new-analysis :discrete-mcc-tree :locations-file] filename))}))

(defn set-readable-name [{:keys [db]} [_ readable-name]]
  {:db (assoc-in db [:new-analysis :discrete-mcc-tree :readable-name] readable-name)})

(defn set-locations-attribute [{:keys [db]} [_ locations-attribute]]
  {:db (assoc-in db [:new-analysis :discrete-mcc-tree :locations-attribute] locations-attribute)})

(defn set-most-recent-sampling-date [{:keys [db]} [_ date]]
  {:db (assoc-in db [:new-analysis :discrete-mcc-tree :most-recent-sampling-date] date)})

(defn set-time-scale-multiplier [{:keys [db]} [_ value]]
  {:db (cond-> db
         true                           (assoc-in [:new-analysis :discrete-mcc-tree :time-scale-multiplier] value)
         (> value 0)                    (dissoc-in [:new-analysis :discrete-mcc-tree :errors :time-scale-multiplier])
         (or (nil? value) (<= value 0)) (assoc-in [:new-analysis :discrete-mcc-tree :errors :time-scale-multiplier] "Set positive value"))})

(defn start-analysis [{:keys [db]} [_ {:keys [readable-name locations-file-url locations-attribute-name
                                              most-recent-sampling-date time-scale-multiplier]}]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:db       (assoc-in db [:analysis id :readable-name] readable-name)
     :dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $name: String!,
                                                     $locationsFileUrl: String!,
                                                     $locationsAttributeName: String!,
                                                     $multiplier: Float!,
                                                     $mrsd: String!) {
                                                   updateDiscreteTree(id: $id,
                                                                        readableName: $name,
                                                                        locationsFileUrl: $locationsFileUrl,
                                                                        locationsAttributeName: $locationsAttributeName,
                                                                        timescaleMultiplier: $multiplier,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     id
                                                     status
                                                   }
                                              }"
                                :variables {:id                     id
                                            :name                   readable-name
                                            :locationsFileUrl       locations-file-url
                                            :locationsAttributeName locations-attribute-name
                                            :multiplier             time-scale-multiplier
                                            :mrsd                   (time/format most-recent-sampling-date)}}]}))
