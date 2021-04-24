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
                                :on-success [:discrete-mcc-tree/s3-upload file-with-meta]}]}))

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
                   (assoc-in [:new-analysis :continuous-mcc-tree :tree-file] filename)
                   ;; default name: file name root
                   (assoc-in [:new-analysis :continuous-mcc-tree :readable-name] readable-name))}))
