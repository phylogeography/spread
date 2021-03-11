(ns ui.home.page
  (:require [ui.router.component :refer [page]]))

(defmethod page :route/home []
  (fn []
    [:div
     [:p "HOME"]]))
