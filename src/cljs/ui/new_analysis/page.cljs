(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [ui.s3 :as s3]
            ;; [ui.component.input :refer [file-drag-input]]
            [taoensso.timbre :as log]
            [ui.component.progress :refer [progress-bar]]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [icons]]
            [ui.router.component :refer [page]]
            [ui.component.button :refer [button-file-upload button-with-icon-and-label]]
            [ui.router.subs :as router.subs]
            [ui.events.graphql :refer [gql->clj]]
            [ui.utils :refer [debounce >evt]]
            [shared.macros :refer [promise->]]
            [clojure.string :as string]))


;;        https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/ebcee862-1168-4110-a96f-0537c8d420b1/
;; TODO : https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/9c18388a-e890-4be4-9c5a-8d5358d86567/

(defn error-reported []

  )

(re-frame/reg-event-fx
  :discrete-mcc-tree/tree-file-upload-progress
  (fn [{:keys [db]} [_ progress]]
    {:db (assoc-in db [:new-analysis :discrete-mcc-tree :tree-file-upload-progress] progress)}))

(re-frame/reg-sub
  :discrete-mcc-tree/tree-file-upload-progress
  (fn [db _]
    (get-in db [:new-analysis :discrete-mcc-tree :tree-file-upload-progress])))

;; TODO
(re-frame/reg-event-fx
  :discrete-mcc-tree/tree-file-upload-success
  (fn [{:keys [db]} [_ {:keys [url filename]}]]
    (let [[url _] (string/split url "?")]

      (prn "@ SUCCESS" url filename)


      {:db db})))

(re-frame/reg-sub
  :discrete-mcc-tree/tree-file
  (fn [db _]
    (get-in db [:new-analysis :discrete-mcc-tree :tree-file])))

(re-frame/reg-event-fx
  :new-analysis/on-tree-file-selected
  (fn [{:keys [db]} [_ file-with-meta]]
    (let [{:keys [data filename]} file-with-meta
          [fname extension]       (string/split filename ".")]

      {:dispatch [:graphql/query {:query
                                  "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                  :variables {:filename fname :extension (or extension "txt")}
                                  :callback  (fn [^js response]
                                               (if (= 200 (.-status response))
                                                 (let [{:keys [get-upload-urls]} (gql->clj (.-data (.-data response)))
                                                       url                       (first get-upload-urls)]
                                                   (s3/upload {:url             url
                                                               :data            data
                                                               ;; TODO
                                                               :on-success      #(>evt [:discrete-mcc-tree/tree-file-upload-success {:url url
                                                                                                                                     :filename filename}])
                                                               ;; TODO : handle error
                                                               :on-error        #(prn "ERROR" %)
                                                               :handle-progress (fn [sent total]
                                                                                  (>evt [:discrete-mcc-tree/tree-file-upload-progress (/ sent total)]))})


                                                   )
                                                 (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))}]})))


(defn discrete-mcc-tree []
  (let [tree-file-upload-progress (re-frame/subscribe [:discrete-mcc-tree/tree-file-upload-progress])
        tree-file (re-frame/subscribe [:discrete-mcc-tree/tree-file])]
    (fn []
      [:div.discrete-mcc-tree
       [:div.upload
        [:span "Load tree file"]
        [:div
         [:div
          [button-file-upload {:icon             :upload
                               :class            "upload-button"
                               :label            "Choose a file"
                               :on-file-accepted #(>evt [:new-analysis/on-tree-file-selected %])}]

          [progress-bar {:class "tree-upload-progress-bar" :progress @tree-file-upload-progress :label "Uploading. Please wait"}]
          [:span.tree-filename @tree-file]]

         [:p
          [:span "When upload is complete all unique attributes will be automatically filled."]
          [:span "You can then select geographical coordinates and change other settings."]]]]])))

(defn discrete-rates []
  [:pre "discrete-rates"])

(defn continuous-mcc-tree []
  [:pre "continuous-mcc-tree"])

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
              [discrete-mcc-tree])]]]]))))
