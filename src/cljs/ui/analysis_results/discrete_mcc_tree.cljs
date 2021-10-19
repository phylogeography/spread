(ns ui.analysis-results.discrete-mcc-tree
  (:require [shared.components :refer [labeled-field]]
            [ui.time :as time]))

(defn discrete-mcc-tree [{:keys [readable-name locations-attribute-name
                                 most-recent-sampling-date timescale-multiplier]}]
  (let [most-recent-sampling-date (when most-recent-sampling-date
                                    (time/format most-recent-sampling-date))]
    [:div.field-table
     [:div.field-line
      [:div.field-card
       [:h4 "Tree file"]
       [labeled-field {:label "Name" :text readable-name}]]]
     [:div.field-line
      [:div.field-card
       [:h4 "Time scale"]
       [labeled-field {:label "Multiplier" :text timescale-multiplier}]]
      [:div.field-card
       [:h4 "Locations"]
       [labeled-field {:label "Attribute name" :text locations-attribute-name}]]]
     [:div.field-line
      [:div.field-card
       [:h4 "Most recent sampling date"]
       [labeled-field {:label "YYYY/MM/DD" :text most-recent-sampling-date}]]]]))
