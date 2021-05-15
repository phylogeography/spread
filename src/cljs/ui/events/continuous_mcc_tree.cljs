(ns ui.events.continuous-mcc-tree
  (:require [clojure.string :as string]
            [ui.s3 :as s3]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

(defn tree-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :tree-file-upload-progress] progress)})

(defn tree-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _]       (string/split url "?")
        readable-name (first (string/split filename "."))]
    {:dispatch [:graphql/query {:query     "mutation UploadContinuousTree($treeFileUrl: String!) {
                                                uploadContinuousTree(treeFileUrl: $treeFileUrl) {
                                                  id
                                                  status
                                                }
                                              }"
                                :variables {:treeFileUrl url}}]
     :db       (-> db
                   (assoc-in [:new-analysis :continuous-mcc-tree :tree-file] filename)
                   ;; default name: file name root
                   (assoc-in [:new-analysis :continuous-mcc-tree :readable-name] readable-name))}))

(defn delete-tree-file [{:keys [db]}]
  ;; TODO : dispatch graphql mutation to delete from db & S3
  {:db (dissoc-in db [:new-analysis :continuous-mcc-tree])})

(defn s3-upload [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:continuous-mcc-tree/tree-file-upload-success {:url      url
                                                                                          :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:continuous-mcc-tree/tree-file-upload-progress (/ sent total)]))}}))

(defn on-tree-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "tree"}
                                :on-success [:continuous-mcc-tree/s3-upload file-with-meta]}]}))

;; TODO: clean analysis fields (dissoc)
(defn start-analysis [{:keys [db]} [_ {:keys [readable-name y-coordinate x-coordinate
                                              hpd-level most-recent-sampling-date time-scale-multiplier]}]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :continuous-tree-parser-id])]
    {:db       (assoc-in db [:continuous-tree-parsers id :readable-name] readable-name)
     :dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                      $x: String!,
                                                      $y: String!,
                                                      $mrsd: String!,
                                                      $name: String!,
                                                      $multiplier: Float!) {
                                                   updateContinuousTree(id: $id,
                                                                        readableName: $name,
                                                                        timescaleMultiplier: $multiplier,
                                                                        xCoordinateAttributeName: $x,
                                                                        yCoordinateAttributeName: $y,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     id
                                                     status
                                                   }
                                              }"
                                :variables {:id         id
                                            :x          x-coordinate
                                            :y          y-coordinate
                                            :multiplier time-scale-multiplier
                                            :name       readable-name
                                            :mrsd       (time/format most-recent-sampling-date)}}]}))

(defn set-readable-name [{:keys [db]} [_ readable-name]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :readable-name] readable-name)})

(defn set-y-coordinate [{:keys [db]} [_ attribute-name]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :y-coordinate] attribute-name)})

(defn set-x-coordinate [{:keys [db]} [_ attribute-name]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :x-coordinate] attribute-name)})

(defn set-most-recent-sampling-date [{:keys [db]} [_ date]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :most-recent-sampling-date] date)})

(defn set-time-scale-multiplier [{:keys [db]} [_ value]]
  {:db (cond-> db
         true                           (assoc-in [:new-analysis :continuous-mcc-tree :time-scale-multiplier] value)
         (> value 0)                    (dissoc-in [:new-analysis :continuous-mcc-tree :errors :time-scale-multiplier])
         (or (nil? value) (<= value 0)) (assoc-in [:new-analysis :continuous-mcc-tree :errors :time-scale-multiplier] "Set positive value"))})
