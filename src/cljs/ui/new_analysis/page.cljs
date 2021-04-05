(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [ui.s3 :as s3]
            [taoensso.timbre :as log]
            [ui.component.input :refer [text-input select-input]]
            [ui.component.progress :refer [progress-bar]]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [icons]]
            [ui.router.component :refer [page]]
            [ui.component.button :refer [button-file-upload button-with-icon button-with-icon-and-label]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.events.graphql :refer [gql->clj]]
            [ui.utils :as ui-utils :refer [debounce >evt dissoc-in split-last]]
            [shared.macros :refer [promise->]]
            [clojure.string :as string]))

;;        https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/ebcee862-1168-4110-a96f-0537c8d420b1/
;;        https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/9c18388a-e890-4be4-9c5a-8d5358d86567/

;; -- SUBS --;;

(re-frame/reg-sub
 :continuous-mcc-tree
 (fn [db]
   (get-in db [:new-analysis :continuous-mcc-tree])))

;; -- EVENTS --;;

(re-frame/reg-event-fx
 :continuous-mcc-tree/tree-file-upload-progress
 (fn [{:keys [db]} [_ progress]]
   {:db (assoc-in db [:new-analysis :continuous-mcc-tree :tree-file-upload-progress] progress)}))

(re-frame/reg-event-fx
 :continuous-mcc-tree/tree-file-upload-success
 (fn [{:keys [db]} [_ {:keys [url filename]}]]
   (let [[url _] (string/split url "?")
         readable-name (first (string/split filename "."))]
     {:dispatch [:graphql/query {:query "mutation UploadContinuousTree($treeFileUrl: String!) {
                                            uploadContinuousTree(treeFileUrl: $treeFileUrl) {
                                              id
                                              status
                                            }
                                          }"
                                 :variables {:treeFileUrl url}}]
      :db (-> db
              (assoc-in [:new-analysis :continuous-mcc-tree :tree-file] filename)
              ;; default name: file name root
              (assoc-in [:new-analysis :continuous-mcc-tree :readable-name] readable-name))})))

(re-frame/reg-event-fx
 :continuous-mcc-tree/delete-tree-file
 (fn [{:keys [db]}]
   ;; TODO : dispatch graphql mutation to delete from db & S3
   {:db (dissoc-in db [:new-analysis :continuous-mcc-tree])}))

(re-frame/reg-event-fx
 :continuous-mcc-tree/on-tree-file-selected
 (fn [{:keys [db]} [_ file-with-meta]]
   (let [{:keys [data filename]} file-with-meta
         splitted       (string/split filename ".")
         fname (first splitted)]
     {:dispatch [:graphql/query {:query
                                 "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                 :variables {:filename fname :extension "tree"}
                                 :callback  (fn [^js response]
                                              (if (= 200 (.-status response))
                                                (let [{:keys [get-upload-urls]} (gql->clj (.-data (.-data response)))
                                                      url                       (first get-upload-urls)]
                                                  (s3/upload {:url             url
                                                              :data            data
                                                              :on-success      #(>evt [:continuous-mcc-tree/tree-file-upload-success {:url      url
                                                                                                                                      :filename filename}])
                                                              ;; TODO : handle error
                                                              :on-error        #(prn "ERROR" %)
                                                              :handle-progress (fn [sent total]
                                                                                 (>evt [:continuous-mcc-tree/tree-file-upload-progress (/ sent total)]))}))
                                                (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))}]})))

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

;; --- PAGE --- ;;

(defn continuous-mcc-tree []
  (let [continuous-mcc-tree (re-frame/subscribe [:continuous-mcc-tree])
        continuous-tree-parser (re-frame/subscribe [::subs/active-continuous-tree-parser])]
    (fn []
      (let [{:keys [id attribute-names hpd-levels] :as server-settings} @continuous-tree-parser
            {:keys [tree-file tree-file-upload-progress readable-name
                    y-coordinate x-coordinate
                    hpd-level]
             :or {y-coordinate (first attribute-names)
                  x-coordinate (first attribute-names)
                  hpd-level (first hpd-levels)}
             :as settings} @continuous-mcc-tree]

        (prn "@1 continuous-mcc-tree / page" settings)
        (prn "@2 continuous-mcc-tree / page" server-settings)

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

         (when attribute-names
           [:div.settings

            [:fieldset
             [:legend "name"]
             [text-input {:value readable-name
                          :on-change #(>evt [:continuous-mcc-tree/set-readable-name %])}]]

            [:div.row
             [:div.column
              [:span "Select y coordinate"]
              [:fieldset
               [:legend "Latitude"]
               [select-input {:value y-coordinate
                              :options attribute-names
                              :on-change #(>evt [:continuous-mcc-tree/set-y-coordinate %])}]]]

             [:div.column
              [:span "Select x coordinate"]
              [:fieldset
               [:legend "Longitude"]
               [select-input {:value x-coordinate
                              :options attribute-names
                              :on-change #(>evt [:continuous-mcc-tree/set-x-coordinate %])}]]]]


            [:div.row
             [:div.column
              [:span "Select HPD level"]
              [:fieldset
               [:legend "Level"]
               [select-input {:value hpd-level
                              :options hpd-levels
                              :on-change #(>evt [:continuous-mcc-tree/set-hpd-level %])}]]]]



            ])




         ]))))

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
