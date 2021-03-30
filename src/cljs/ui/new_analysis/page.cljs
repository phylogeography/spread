(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.component.app-container :refer [app-container]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]))

;; TODO : https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/9c18388a-e890-4be4-9c5a-8d5358d86567/

(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (log/debug "app-layout/active-page" active-page)
      [app-container
       [:div.new-analysis
        [:p "NEW-ANALYSIS"]]])))
