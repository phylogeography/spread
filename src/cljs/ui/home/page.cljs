(ns ui.home.page
  (:require [ui.router.component :refer [page]]
            [ui.graphql :as graphql]))

;; (rf/dispatch [::wfx/request socket-id request])

(defmethod page :route/home []
  (fn []
    [:div
     [:p "HOME"]]))
