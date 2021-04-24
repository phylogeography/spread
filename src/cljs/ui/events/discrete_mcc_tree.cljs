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
  ;; TODO : dispatch graphql mutation to delete from db & S3
  {:db (dissoc-in db [:new-analysis :discrete-mcc-tree])})

(defn delete-locations-file [{:keys [db]}]
  ;; TODO : dispatch graphql mutation to delete from db & S3
  {:db (dissoc-in db [:new-analysis :discrete-mcc-tree :locations-file])})

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
  (let [[url _]       (string/split url "?")
        readable-name (first (string/split filename "."))]
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
