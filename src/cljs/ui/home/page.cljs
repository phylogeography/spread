(ns ui.home.page
  (:require [re-frame.core :as re-frame]
            [ui.component.app-container :refer [app-container]]
            [ui.component.button
             :refer
             [button-with-icon-and-label button-with-label]]
            [ui.component.icon :refer [icon-with-label icons]]
            [ui.router.component :refer [page]]))

(defmethod page :route/home []
  (fn []
    [app-container

     [:div [:span "HOME"]]

     #_[:div.home
      [icon-with-label {:icon (:spread icons) :label "spread"}]
      [:b "Welcome to Spread"]
      [:p "System for visualizing phylogeographic reconstructions."]
      [:p "Click on the button below and follow the given directions."]
      [button-with-icon-and-label {:class    "analysis-button"
                                   :icon     (:run-analysis icons)
                                   :label    "Run new analysis"
                                   :on-click #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])}]
      [:hr.horizontal-divider]
      [:p "Check the documentation for further informations."]
      [button-with-label {:class    "documentation-button"
                          :label    "Read documentation"
                          :on-click #(re-frame/dispatch [:router/navigate :route/documentation])}]]]))
