(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.component.app-container :refer [app-container]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]))

(defmethod page :route/analysis-results []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]} @active-page]
        [app-container
         [:div.analysis-results
          [:p "ANALYSIS RESULTS: " (:id query)]]]))))
