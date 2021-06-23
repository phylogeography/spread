(ns ui.analysis-results.discrete-rates
  (:require [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]))

(defn discrete-rates [{:keys [readable-name burn-in] :as analysis}
                      classes]
  [grid {:container true
         :direction :column
         :spacing   1}
   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [typography {:class-name (:input-label classes)} "Name"]]
    [grid {:item true :xs 6 :xm 6}]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [text-field {:label   "Name"
                 :variant :outlined
                 :value   readable-name}]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [typography {:class-name (:input-label classes)} "Burn in"]]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [text-field {:label   "Burn in"
                  :variant :outlined
                  :value   burn-in}]]]])
