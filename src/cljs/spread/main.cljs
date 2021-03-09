(ns spread.main
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [spread.views.main-screen :as main-screen]
            [spread.events :as events]
            [spread.fxs]
            ))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (rdom/render [main-screen/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize])
  (mount-root))

