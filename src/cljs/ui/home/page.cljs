(ns ui.home.page
  (:require [ui.router.events :as router-events]
            [ui.router.subs :as router.subs]
            [ui.router.component :refer [page]]
            [re-frame.core :as re]
            [reagent.core :as r]))

(defmethod page :route/home []
  (let [active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      [:div
       [:p "HOME"]])))
