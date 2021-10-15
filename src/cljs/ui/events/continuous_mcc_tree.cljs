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
    {:dispatch [:graphql/query {:query
                                "mutation UploadContinuousTreeDiscreteTree($treeFileUrl: String!, $treeFileName: String!) {
                                              uploadContinuousTree(treeFileUrl: $treeFileUrl, treeFileName: $treeFileName) {
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
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                   deleteAnalysis(id: $analysisId) {
                                          id
                                        }
                                     }"
                                :variables {:analysisId id}}]
     :db       (dissoc-in db [:new-analysis :continuous-mcc-tree])}))

(defn upload-tree-file [_ [_ {:keys [data filename]} response]]
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
                                :on-success [:continuous-mcc-tree/upload-tree-file file-with-meta]}]}))

(defn on-trees-file-selected [_ [_ file-with-meta]]
  (let [{:keys [filename]} file-with-meta
        splitted           (string/split filename ".")
        fname              (first splitted)]
    {:dispatch [:graphql/query {:query
                                "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                :variables  {:filename fname :extension "trees"}
                                :on-success [:continuous-mcc-tree/upload-trees-file file-with-meta]}]}))

(defn upload-trees-file [_ [_ {:keys [data filename]} response]]
  (let [url (-> response :data :getUploadUrls first)]
    {::s3/upload {:url             url
                  :data            data
                  :on-success      #(>evt [:continuous-mcc-tree/trees-file-upload-success {:url      url
                                                                                           :filename filename}])
                  ;; TODO : handle error
                  :on-error        #(prn "ERROR" %)
                  :handle-progress (fn [sent total]
                                     (>evt [:continuous-mcc-tree/trees-file-upload-progress (/ sent total)]))}}))

(defn trees-file-upload-success [{:keys [db]} [_ {:keys [url filename]}]]
  (let [[url _]            (string/split url "?")
        continuous-tree-id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UploadTimeSlicer($continuousTreeId: ID!, $url: String!, $name: String!) {
                                                   uploadTimeSlicer(continuousTreeId: $continuousTreeId,
                                                                    treesFileUrl: $url,
                                                                    treesFileName: $name) {
                                                     id
                                                     continuousTreeId
                                                     status
                                                     treesFileName
                                                   }
                                                }"
                                :variables {:url              url
                                            :name             filename
                                            :continuousTreeId continuous-tree-id}}]}))

(defn trees-file-upload-progress [{:keys [db]} [_ progress]]
  {:db (assoc-in db [:new-analysis :continuous-mcc-tree :trees-file-upload-progress] progress)})

(defn delete-trees-file [{:keys [db]}]
  ;; NOTE: internally this is a separate parser therefore we need to delete the whole analysis
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :time-slicer-parser-id])]
    {:dispatch [:graphql/query {:query
                                "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                   deleteAnalysis(id: $analysisId) {
                                          id
                                        }
                                     }"
                                :variables {:analysisId id}}]
     :db       (-> db
                   (dissoc-in [:new-analysis :continuous-mcc-tree :trees-file])
                   (dissoc-in [:new-analysis :continuous-mcc-tree :trees-file-upload-progress]))}))

(defn set-readable-name [{:keys [db]} [_ readable-name]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $readableName: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        readableName: $readableName) {
                                                     id
                                                     status
                                                     readableName
                                                   }
                                              }"
                                :variables {:id           id
                                            :readableName readable-name}}]}))

(defn set-y-coordinate [{:keys [db]} [_ attribute-name]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $y: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        yCoordinateAttributeName: $y) {
                                                     id
                                                     status
                                                     yCoordinateAttributeName
                                                   }
                                              }"
                                :variables {:id id
                                            :y  attribute-name}}]}))

(defn set-x-coordinate [{:keys [db]} [_ attribute-name]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $x: String!) {
                                                   updateContinuousTree(id: $id,
                                                                        xCoordinateAttributeName: $x) {
                                                     id
                                                     status
                                                     xCoordinateAttributeName
                                                   }
                                              }"
                                :variables {:id id
                                            :x  attribute-name}}]}))

(defn set-most-recent-sampling-date [{:keys [db]} [_ date]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch [:graphql/query {:query
                                "mutation UpdateTree($id: ID!,
                                                     $mostRecentSamplingDate: String!) {
                                   updateContinuousTree(id: $id,
                                                        mostRecentSamplingDate: $mostRecentSamplingDate) {
                                     id
                                     status
                                     mostRecentSamplingDate
                                   }
                                 }"
                                :variables {:id                     id
                                            :mostRecentSamplingDate (time/format date)}}]}))

(defn set-time-scale-multiplier [{:keys [db]} [_ value]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    (merge {:db (cond-> db
                  true                           (assoc-in [:analysis id :timescale-multiplier] value)
                  (> value 0)                    (dissoc-in [:new-analysis :continuous-mcc-tree :errors :timescale-multiplier])
                  (or (nil? value) (<= value 0)) (assoc-in [:new-analysis :continuous-mcc-tree :errors :timescale-multiplier] "Set positive value"))}
           (when (> value 0)
             {:dispatch [:graphql/query {:query
                                         "mutation UpdateTree($id: ID!,
                                                              $timescaleMultiplier: Float!) {
                                            updateContinuousTree(id: $id,
                                                                 timescaleMultiplier: $timescaleMultiplier) {
                                              id
                                              status
                                              timescaleMultiplier
                                              mostRecentSamplingDate
                                            }
                                          }"
                                         :variables {:id                  id
                                                     :timescaleMultiplier value}}]}))))

(defn start-analysis [{:keys [db]} [_ {:keys [readable-name
                                              y-coordinate-attribute-name x-coordinate-attribute-name
                                              most-recent-sampling-date timescale-multiplier] :as all}]]
  (let [id (get-in db [:new-analysis :continuous-mcc-tree :id])]
    {:dispatch-n [[:graphql/subscription {:id        id
                                          :query     "subscription SubscriptionRoot($id: ID!) {
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
                                                      $x: String!,
                                                      $y: String!,
                                                      $mrsd: String!,
                                                      $name: String!,
                                                      $multiplier: Float!) {
                                                   startContinuousTreeParser(id: $id,
                                                                             readableName: $name,
                                                                             timescaleMultiplier: $multiplier,
                                                                             xCoordinateAttributeName: $x,
                                                                             yCoordinateAttributeName: $y,
                                                                             mostRecentSamplingDate: $mrsd) {
                                                     id
                                                     status
                                                     readableName
                                                     timescaleMultiplier
                                                     xCoordinateAttributeName
                                                     yCoordinateAttributeName
                                                     mostRecentSamplingDate
                                                   }
                                              }"
                                   :variables {:id         id
                                               :x          x-coordinate-attribute-name
                                               :y          y-coordinate-attribute-name
                                               :multiplier timescale-multiplier
                                               :name       readable-name
                                               :mrsd       (time/format most-recent-sampling-date)}}]]}))
