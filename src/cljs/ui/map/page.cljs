(ns ui.map.page  
  (:require [re-frame.core :as re-frame]      
            [reagent.core :as reagent]
            [ui.component.maps :as maps]            
            [ui.router.component :refer [page]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dummy page to test map functionality ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod page :route/map []
  (let [analysis-id (reagent/atom "3a6cc419-5da4-4d28-8b0d-f74c98a89d6e")]
    (fn []      
      [:div
       [:p "Map"]
       [:input {:type :text
                :value @analysis-id
                :on-change #(reset! analysis-id (.-value (.-target %)))}]
       [:button {:on-click #(re-frame/dispatch [:analysis/load-continuous-tree-analysis @analysis-id])}
        "Load analysis"]
       [maps/animated-data-map]])))
