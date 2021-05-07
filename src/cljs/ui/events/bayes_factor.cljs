(ns ui.events.bayes-factor
  (:require [clojure.string :as string]
            [ui.s3 :as s3]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

(defn on-log-file-selected [_ [_ file-with-meta]]

  (prn "@@@ on-log-file-selected")

  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "log"}
                                :on-success [:bayes-factor/s3-log-file-upload file-with-meta]}]}))

(defn s3-log-file-upload [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:bayes-factor/log-file-upload-success {:url      url
                                                                                  :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:bayes-factor/log-file-upload-progress (/ sent total)]))}}))

(defn log-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :bayes-factor :log-file-upload-progress] progress)})

(defn delete-log-file [{:keys [db]}]
  ;; TODO : dispatch graphql mutation to delete from db & S3
  {:db (dissoc-in db [:new-analysis :bayes-factor])})

(defn delete-locations-file [{:keys [db]}]
  ;; TODO : dispatch graphql mutation to delete from db & S3
  {:db (dissoc-in db [:new-analysis :bayes-factor :locations-file])})

(defn log-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _]       (string/split url "?")
        readable-name (first (string/split filename "."))]
    {:dispatch [:graphql/query {:query     "mutation UploadBayesFactor($logUrl: String!) {
                                                   uploadBayesFactorAnalysis(logFileUrl: $logUrl) {
                                                     id
                                                     status
                                                  }
                                                }"
                                :variables {:logUrl url}}]
     :db       (-> db
                   (assoc-in [:new-analysis :bayes-factor :log-file] filename)
                   ;; default name: file name root
                   (assoc-in [:new-analysis :bayes-factor :readable-name] readable-name))}))

(defn on-locations-file-selected [_ [_ file-with-meta]]

  (prn "@@@ on-locations-file-selected")

  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "txt"}
                                :on-success [:bayes-factor/s3-locations-file-upload file-with-meta]}]}))

(defn s3-locations-file-upload [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:bayes-factor/locations-file-upload-success {:url      url
                                                                                        :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:bayes-factor/locations-file-upload-progress (/ sent total)]))}}))

(defn locations-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :bayes-factor :locations-file-upload-progress] progress)})

(defn locations-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _] (string/split url "?")]
    {:db (-> db
             (assoc-in [:new-analysis :bayes-factor :locations-file-url] url)
             (assoc-in [:new-analysis :bayes-factor :locations-file] filename))}))
