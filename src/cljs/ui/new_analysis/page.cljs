(ns ui.new-analysis.page
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button
             :refer
             [button-file-upload button-with-icon button-with-label]]
            [ui.component.date-picker :refer [date-picker]]
            [ui.component.indicator :refer [busy]]
            [ui.component.input :refer [amount-input select-input text-input]]
            [ui.component.progress :refer [progress-bar]]
            [ui.events.graphql :refer [gql->clj]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.s3 :as s3]
            [ui.subscriptions :as subs]
            [ui.time :as time]
            [ui.utils :as ui-utils :refer [>evt dissoc-in]]))

;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/ebcee862-1168-4110-a96f-0537c8d420b1/
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/9c18388a-e890-4be4-9c5a-8d5358d86567/

;; -- SUBS --;;

(re-frame/reg-sub
  :continuous-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree])))

(re-frame/reg-sub
  :continuous-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree :errors])))

;; -- EVENTS --;;

(re-frame/reg-event-fx
  :continuous-mcc-tree/tree-file-upload-progress
  (fn [{:keys [db]} [_ progress]]
    {:db (assoc-in db [:new-analysis :continuous-mcc-tree :tree-file-upload-progress] progress)}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/tree-file-upload-success
  (fn [{:keys [db]} [_ {:keys [url filename]}]]
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
                     (assoc-in [:new-analysis :continuous-mcc-tree :readable-name] readable-name))})))

(re-frame/reg-event-fx
  :continuous-mcc-tree/delete-tree-file
  (fn [{:keys [db]}]
    ;; TODO : dispatch graphql mutation to delete from db & S3
    {:db (dissoc-in db [:new-analysis :continuous-mcc-tree])}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/s3-upload
  (fn [{:keys [db]} [_ {:keys [data filename]} response]]
    (let [url (-> response :data :getUploadUrls first)]
      {::s3/upload {:url             url
                    :data            data
                    :on-success      #(>evt [:continuous-mcc-tree/tree-file-upload-success {:url      url
                                                                                            :filename filename}])
                    ;; TODO : handle error
                    :on-error        #(prn "ERROR" %)
                    :handle-progress (fn [sent total]
                                       (>evt [:continuous-mcc-tree/tree-file-upload-progress (/ sent total)]))}})))

(re-frame/reg-event-fx
  :continuous-mcc-tree/on-tree-file-selected
  (fn [_ [_ file-with-meta]]
    (let [{:keys [data filename]} file-with-meta
          splitted                (string/split filename ".")
          fname                   (first splitted)]
      {:dispatch [:graphql/query {:query
                                  "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                  :variables {:filename fname :extension "tree"}
                                  :on-success [:continuous-mcc-tree/s3-upload file-with-meta]}]})))

;; TODO: clean analysis fields (dissoc)
(re-frame/reg-event-fx
  :continuous-mcc-tree/start-analysis
  (fn [{:keys [db]} [_ {:keys [readable-name y-coordinate x-coordinate
                               hpd-level most-recent-sampling-date time-scale-multiplier]}]]
    (let [id (get-in db [:new-analysis :continuous-mcc-tree :continuous-tree-parser-id])]
      {:db       (assoc-in db [:continuous-tree-parsers id :readable-name] readable-name)
       :dispatch [:graphql/query {:query
                                  "mutation UpdateTree($id: ID!,
                                                       $x: String!,
                                                       $y: String!,
                                                       $hpd: String!,
                                                       $mrsd: String!,
                                                       $name: String!,
                                                       $multiplier: Float!) {
                                                   updateContinuousTree(id: $id,
                                                                        readableName: $name,
                                                                        timescaleMultiplier: $multiplier,
                                                                        xCoordinateAttributeName: $x,
                                                                        yCoordinateAttributeName: $y,
                                                                        hpdLevel: $hpd,
                                                                        mostRecentSamplingDate: $mrsd) {
                                                     id
                                                     status
                                                   }
                                              }"
                                  :variables {:id         id
                                              :x          x-coordinate
                                              :y          y-coordinate
                                              :multiplier time-scale-multiplier
                                              :hpd        hpd-level
                                              :name       readable-name
                                              :mrsd       (time/format most-recent-sampling-date)}}]})))

(re-frame/reg-event-fx
  :continuous-mcc-tree/set-readable-name
  (fn [{:keys [db]} [_ readable-name]]
    {:db (assoc-in db [:new-analysis :continuous-mcc-tree :readable-name] readable-name)}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/set-y-coordinate
  (fn [{:keys [db]} [_ attribute-name]]
    {:db (assoc-in db [:new-analysis :continuous-mcc-tree :y-coordinate] attribute-name)}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/set-x-coordinate
  (fn [{:keys [db]} [_ attribute-name]]
    {:db (assoc-in db [:new-analysis :continuous-mcc-tree :x-coordinate] attribute-name)}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/set-most-recent-sampling-date
  (fn [{:keys [db]} [_ date]]
    {:db (assoc-in db [:new-analysis :continuous-mcc-tree :most-recent-sampling-date] date)}))

(re-frame/reg-event-fx
  :continuous-mcc-tree/set-time-scale-multiplier
  (fn [{:keys [db]} [_ value]]
    {:db (cond-> db
           true                           (assoc-in [:new-analysis :continuous-mcc-tree :time-scale-multiplier] value)
           (> value 0)                    (dissoc-in [:new-analysis :continuous-mcc-tree :errors :time-scale-multiplier])
           (or (nil? value) (<= value 0)) (assoc-in [:new-analysis :continuous-mcc-tree :errors :time-scale-multiplier] "Set positive value"))}))

;; --- PAGE --- ;;

(defn error-reported [message]
  (when message
    [:div.error-reported
     [:span message]]))

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree    (re-frame/subscribe [:continuous-mcc-tree])
        continuous-tree-parser (re-frame/subscribe [::subs/active-continuous-tree-parser])
        field-errors           (re-frame/subscribe [:continuous-mcc-tree-field-errors])]
    (fn []
      (let [{:keys [attribute-names hpd-levels]} @continuous-tree-parser
            {:keys [tree-file tree-file-upload-progress readable-name
                    y-coordinate x-coordinate
                    hpd-level most-recent-sampling-date
                    time-scale-multiplier]
             :or   {y-coordinate              (first attribute-names)
                    x-coordinate              (first attribute-names)
                    hpd-level                 (first hpd-levels)
                    most-recent-sampling-date (time/now)
                    time-scale-multiplier     1}}
            @continuous-mcc-tree]
        [:div.continuous-mcc-tree
         [:div.upload
          [:span "Load tree file"]
          [:div
           [:div
            (cond
              (and (nil? tree-file-upload-progress) (nil? tree-file))
              [button-file-upload {:icon             :upload
                                   :class            "upload-button"
                                   :label            "Choose a file"
                                   :on-file-accepted #(>evt [:continuous-mcc-tree/on-tree-file-selected %])}]

              (not= 1 tree-file-upload-progress)
              [progress-bar {:class "tree-upload-progress-bar" :progress tree-file-upload-progress :label "Uploading. Please wait"}]

              :else [:span.tree-filename tree-file])]

           (if (nil? tree-file)
             [:p
              [:span "When upload is complete all unique attributes will be automatically filled."]
              [:span "You can then select geographical coordinates and change other settings."]]
             [button-with-icon {:on-click #(>evt [:continuous-mcc-tree/delete-tree-file])
                                :icon     :delete}])]]

         [:div.settings
          ;; show indicator before worker parses the attributes
          (when (and (= 1 tree-file-upload-progress) (nil? attribute-names))
            [busy])

          (when attribute-names
            [:<>
             [:fieldset
              [:legend "name"]
              [text-input {:value     readable-name
                           :on-change #(>evt [:continuous-mcc-tree/set-readable-name %])}]]

             [:div.row
              [:div.column
               [:span "Select y coordinate"]
               [:fieldset
                [:legend "Latitude"]
                [select-input {:value     y-coordinate
                               :options   attribute-names
                               :on-change #(>evt [:continuous-mcc-tree/set-y-coordinate %])}]]]
              [:div.column
               [:span "Select x coordinate"]
               [:fieldset
                [:legend "Longitude"]
                [select-input {:value     x-coordinate
                               :options   attribute-names
                               :on-change #(>evt [:continuous-mcc-tree/set-x-coordinate %])}]]]]

             [:div.row
              [:div.column
               [:span "Select HPD level"]
               [:fieldset
                [:legend "Level"]
                [select-input {:value     hpd-level
                               :options   hpd-levels
                               :on-change #(>evt [:continuous-mcc-tree/set-hpd-level %])}]]]
              [:div.column
               [:span "Most recent sampling date"]
               [date-picker {:date-format time/date-format
                             :on-change   #(>evt [:continuous-mcc-tree/set-most-recent-sampling-date %])
                             :selected    most-recent-sampling-date}]]]

             [:div.row
              [:div.column
               [:span "Time scale"]
               [:fieldset
                [:legend "Multiplier"]
                [amount-input {:class     :multiplier-field
                               :value     time-scale-multiplier
                               :on-change #(>evt [:continuous-mcc-tree/set-time-scale-multiplier %])}]]
               [error-reported (:time-scale-multiplier @field-errors)]]]

             [:div.start-analysis-section
              [button-with-label {:label     "Start analysis"
                                  :class     :button-start-analysis
                                  :disabled? (seq @field-errors)
                                  :on-click  #(>evt [:continuous-mcc-tree/start-analysis {:readable-name             readable-name
                                                                                          :y-coordinate              y-coordinate
                                                                                          :x-coordinate              x-coordinate
                                                                                          :hpd-level                 hpd-level
                                                                                          :most-recent-sampling-date most-recent-sampling-date
                                                                                          :time-scale-multiplier     time-scale-multiplier}])}]
              [button-with-label {:label    "Paste settings"
                                  :class    :button-paste-settings
                                  :on-click #(prn "TODO : paste settings")}]
              [button-with-label {:label    "Reset"
                                  :class    :button-reset
                                  :on-click #(prn "TODO : reset")}]]])]]))))

(defn discrete-mcc-tree []
  [:pre "discrete mcc tree"])

(defn discrete-rates []
  [:pre "discrete-rates"])

(defn continuous-time-slices []
  [:pre "continuous-time-slices"])

(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]}   @active-page
            {active-tab :tab} query]
        [app-container
         [:div.new-analysis
          [:span "Run new analysis"]
          [:div.tabbed-pane
           [:div.tabs
            (map (fn [tab]
                   [:button.tab {:class    (when (= active-tab tab) "active")
                                 :key      tab
                                 :on-click #(>evt [:router/navigate :route/new-analysis nil {:tab tab}])}
                    (case tab
                      "discrete-mcc-tree"
                      [:div
                       [:span "Discrete"]
                       [:span "MCC tree"]]

                      "discrete-rates"
                      [:div
                       [:span "Discrete"]
                       [:span "Rates"]]

                      "continuous-mcc-tree"
                      [:div
                       [:span "Continuous"]
                       [:span "MCC tree"]]

                      "continuous-time-slices"
                      [:div
                       [:span "Continuous"]
                       [:span "Time slices"]]
                      nil)])
                 ["discrete-mcc-tree" "discrete-rates" "continuous-mcc-tree" "continuous-time-slices"])]
           [:div.panel
            (case active-tab
              "discrete-mcc-tree"      [discrete-mcc-tree]
              "discrete-rates"         [discrete-rates]
              "continuous-mcc-tree"    [continuous-mcc-tree]
              "continuous-time-slices" [continuous-time-slices]
              [continuous-mcc-tree])]]]]))))
