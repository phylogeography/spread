(ns ui.analysis-results.page
  (:require [re-frame.core :as re-frame]
            [ui.component.app-container :refer [app-container]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]))

;; NOTE : just the results tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed3112972c3f62905120

;; TODO : data+ error tab
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed305a09c542e790702f

(defmethod page :route/analysis-results []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]} @active-page]
        [app-container
         [:div.analysis-results
          [:p "ANALYSIS RESULTS: " (:id query)]]]))))
