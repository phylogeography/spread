(ns ui.events.discrete-mcc-tree
  (:require [clojure.string :as string]
            [ui.s3 :as s3]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

(defn reset [{:keys [db]} _]
  {:dispatch [:router/navigate :route/new-analysis nil {:tab "discrete-mcc-tree"}]
   :db (dissoc-in db [:new-analysis :discrete-mcc-tree])})

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

(defn tree-file-upload-success [_ [_ {:keys [url filename]}]]
  (let [[url _] (string/split url "?")]
    {:dispatch [:graphql/query {:query     "mutation UploadDiscreteTree($treeFileUrl: String!, $treeFileName: String!) {
                                              uploadDiscreteTree(treeFileUrl: $treeFileUrl, treeFileName: $treeFileName) {
                                                id
                                                status
                                                readableName
                                                treeFileName
                                                createdOn
                                                }
                                              }"
                                :variables {:treeFileUrl  url
                                            :treeFileName filename}}]}))

(defn delete-tree-file [{:keys [db]}]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                   deleteAnalysis(id: $analysisId) {
                                     id
                                   }
                                 }"
                                :variables {:analysisId id}}]
     :db       (-> db
                   (dissoc-in [:analysis id])
                   (dissoc-in [:new-analysis :discrete-mcc-tree]))}))

(defn delete-locations-file [{:keys [db]}]
  (let [id                           (get-in db [:new-analysis :discrete-mcc-tree :id])
        {:keys [locations-file-url]} (get-in db [:new-analysis :discrete-mcc-tree])]
    {:dispatch [:graphql/query {:query
                                ;; TODO : delete DB content too! [another mutation needed!]
                                "mutation DeleteFile($url: String!) {
                                   deleteFile(url: $url) {
                                     key
                                   }
                                 }"
                                :variables {:url locations-file-url}}]
     :db       (-> db
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file-url])
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file-name])
                   (dissoc-in [:new-analysis :discrete-mcc-tree :locations-file-upload-progress])
                   (dissoc-in [:analysis id :locations-file-url])
                   (dissoc-in [:analysis id :locations-file-name])
                   (dissoc-in [:analysis id :locations-file-upload-progress]))}))

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
  (let [[url _] (string/split url "?")
        id      (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $locationsFileUrl: String!,
                                                     $locationsFileName: String!) {
                                                   updateDiscreteTree(id: $id,
                                                                      locationsFileUrl: $locationsFileUrl,
                                                                      locationsFileName: $locationsFileName) {
                                                     id
                                                     status
                                                     locationsFileUrl
                                                     locationsFileName
                                                   }
                                              }"
                                :variables {:id                id
                                            :locationsFileUrl  url
                                            :locationsFileName filename}}]
     :db (assoc-in db [:new-analysis :discrete-mcc-tree :locations-file-url] url)}))

(defn set-readable-name [{:keys [db]} [_ readable-name]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $readableName: String!) {
                                                   updateDiscreteTree(id: $id,
                                                                      readableName: $readableName) {
                                                     id
                                                     status
                                                     readableName
                                                   }
                                              }"
                                :variables {:id           id
                                            :readableName readable-name}}]}))

(defn set-locations-attribute [{:keys [db]} [_ locations-attribute]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $locationsAttributeName: String!) {
                                                   updateDiscreteTree(id: $id,
                                                                      locationsAttributeName: $locationsAttributeName) {
                                                     id
                                                     status
                                                     locationsAttributeName
                                                   }
                                              }"
                                :variables {:id                     id
                                            :locationsAttributeName locations-attribute}}]}))

(defn set-most-recent-sampling-date [{:keys [db]} [_ most-recent-sampling-date]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $mostRecentSamplingDate: String!) {
                                   updateDiscreteTree(id: $id,
                                                      mostRecentSamplingDate: $mostRecentSamplingDate) {
                                     id
                                     status
                                     mostRecentSamplingDate
                                   }
                                 }"
                                :variables {:id                     id
                                            :mostRecentSamplingDate (time/format most-recent-sampling-date)}}]}))

(defn set-time-scale-multiplier [{:keys [db]} [_ value]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])
        tsm (let [nval (ui-utils/fully-typed-number value)]
              (when (pos? nval) nval))]
    (merge {:db (cond-> db
                  true       (assoc-in [:analysis id :timescale-multiplier] value)
                  tsm        (dissoc-in [:new-analysis :discrete-mcc-tree :errors :timescale-multiplier])
                  (nil? tsm) (assoc-in [:new-analysis :discrete-mcc-tree :errors :timescale-multiplier] "Set positive value"))}
           (when tsm
             {:dispatch [:graphql/query {:query
                                         "mutation UpdateTree($id: ID!,
                                                              $timescaleMultiplier: Float!) {
                                            updateDiscreteTree(id: $id,
                                                               timescaleMultiplier: $timescaleMultiplier) {
                                              id
                                              status
                                              timescaleMultiplier
                                              mostRecentSamplingDate
                                            }
                                          }"
                                         :variables {:id                  id
                                                     :timescaleMultiplier value}}]}))))

(defn start-analysis [{:keys [db]} [_ {:keys [readable-name locations-attribute-name
                                              most-recent-sampling-date timescale-multiplier]}]]
  (let [id (get-in db [:new-analysis :discrete-mcc-tree :id])]
    {:dispatch-n [[:graphql/subscription {:id        id
                                          :query
                                          "subscription SubscriptionRoot($id: ID!) {
                                             parserStatus(id: $id) {
                                               id
                                               status
                                               progress
                                               ofType
                                             }
                                           }"
                                          :variables {"id" id}}]
                  [:graphql/query {:query
                                   "mutation QueueJob($id: ID!,
                                                      $readableName: String!,
                                                      $locationsAttributeName: String!,
                                                      $mostRecentSamplingDate: String!,
                                                      $timescaleMultiplier: Float!) {
                                               startDiscreteTreeParser(id: $id
                                                                       readableName: $readableName,
                                                                       locationsAttributeName: $locationsAttributeName,
                                                                       mostRecentSamplingDate: $mostRecentSamplingDate,
                                                                       timescaleMultiplier: $timescaleMultiplier) {
                                                 id
                                                 status
                                                 readableName
                                                 locationsAttributeName
                                                 mostRecentSamplingDate
                                                 timescaleMultiplier
                                               }
                                             }"
                                   :variables {:id                     id
                                               :readableName           readable-name
                                               :locationsAttributeName locations-attribute-name
                                               :timescaleMultiplier    timescale-multiplier
                                               :mostRecentSamplingDate (time/format most-recent-sampling-date)}}]]}))
