(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [ui.s3 :as s3]
            ;; [ui.component.input :refer [file-drag-input]]
            [taoensso.timbre :as log]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [icons]]
            [ui.router.component :refer [page]]
            [ui.component.button :refer [button-file-input button-with-icon-and-label]]
            [ui.router.subs :as router.subs]
            [ui.events.graphql :refer [gql->clj]]
            [clojure.string :as string]))

;; TODO : https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/9c18388a-e890-4be4-9c5a-8d5358d86567/

(defn error-reported []

  )

(re-frame/reg-event-fx
  :new-analysis/on-tree-file-selected
  (fn [{:keys [db]} [_ file-with-meta]]
    (let [{:keys [data filename]} file-with-meta
          [fname extension]       (string/split filename ".")]
      (prn "@new-analysis/on-tree-file-selected")
      {:dispatch [:graphql/query {:query
                                  "mutation GetUploadUrls($filename: String!, $extension: String!) {
                                     getUploadUrls(files: [ {name: $filename, extension: $extension }])
                                   }"
                                  :variables {:filename fname :extension (or extension ".txt")}
                                  :callback  (fn [^js response]
                                               (if (= 200 (.-status response))
                                                 (let [{:keys [get-upload-urls]} (gql->clj (.-data (.-data response)))
                                                       url                       (first get-upload-urls)]
                                                   (s3/upload {:url             url
                                                               :data            data
                                                               :on-success      #(prn "SUCCESS" %)
                                                               :on-error        #(prn "ERROR" %)
                                                               :handle-progress (fn [sent total] (prn "PROGRESS" (/ sent total)))}))
                                                 (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))}]})))

(defn discrete-mcc-tree []
  (fn []

    [:div.discrete-mcc-tree
     [:div.upload
      [:span "Load tree file"]
      [:div
       [button-file-input {:icon             :upload
                           :class            "upload-button"
                           :label            "Choose a file"
                           :on-file-accepted #(re-frame/dispatch [:new-analysis/on-tree-file-selected %])}]
       [:p
        [:span "When upload is complete all unique attributes will be automatically filled."]
        [:span "You can then select geographical coordinates and change other settings."]]]]

     ]

    ))

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
                                 :on-click #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab tab}])}
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
