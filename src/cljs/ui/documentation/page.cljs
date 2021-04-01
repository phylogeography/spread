(ns ui.documentation.page
  (:require [ui.component.app-container :refer [app-container]]
            [ui.router.component :refer [page]]))

(defmethod page :route/documentation []
  (fn []
    [app-container
     [:div.documentation
      [:pre "Documentation page"]]]))
