(ns ui.splash.page
  (:require [ui.router.events :as router-events]
            [ui.router.subs :as router.subs]
            [ui.router.component :refer [page]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [reagent.core :as r]))

;; TODO : google login
(defmethod page :route/splash []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]

    (log/debug "@@@ route/splash" active-page)

    (fn []
      [:div
       [:p "SPLASH"]])))
