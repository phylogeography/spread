(ns ui.home.page
  (:require [ui.router.component :refer [page]]
            [re-frame.core :as re-frame]))

(defmethod page :route/home []
  (fn []
    [:div
     [:p "HOME"]]))
