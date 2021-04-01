(ns ui.documentation.page
  (:require [ui.router.component :refer [page]]
            [ui.component.app-container :refer [app-container]]))

(defmethod page :route/documentation []
  (fn []
    [app-container
     [:div.documentation
      [:pre "Documentation page"]]]))
