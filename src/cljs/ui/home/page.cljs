(ns ui.home.page
  (:require [re-frame.core :as re-frame]
            [ui.router.component :refer [page]]
            [ui.subscriptions :as subs]
            [ui.component.button :refer [button-with-icon-and-label button-with-label]]
            [ui.component.icon :refer [icons icon-with-label]]
            [ui.component.app-container :refer [app-container]]))

(def analysis-id "db6969bc-bf87-4ebe-919b-ff377bfe5992")

(defmethod page :route/home []
  (let [analysis-status (re-frame/subscribe [::subs/discrete-tree-parser analysis-id])]
    (fn []
      (let [{:keys [status]} @analysis-status]
        [app-container
         [:div.home
          [icon-with-label {:icon (:spread icons) :label "spread"}]
          [:b "Welcome to Spread"]
          [:p "System for visualizing phylogeographic reconstructions."]
          [:p "Click on the button below and follow the given directions."]
          [button-with-icon-and-label {:class    "analysis-button"
                                       :icon     (:run-analysis icons)
                                       :label    "Run new analysis"
                                       :on-click #(re-frame/dispatch [:router/navigate :route/new-analysis])}]

          [:hr.horizontal-divider]

          [:p "Check the documentation for further informations."]

          [button-with-label {:class    "documentation-button"
                              :label    "Read documentation"
                              :on-click #(re-frame/dispatch [:router/navigate :route/documentation])}]

          [:pre (str "POC " analysis-id ": " status)]]]))))
