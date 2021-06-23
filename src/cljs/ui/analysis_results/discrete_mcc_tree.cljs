(ns ui.analysis-results.discrete-mcc-tree
  (:require [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.typography :refer [typography]]))

(defn discrete-mcc-tree [{:keys [readable-name locations-attribute-name
                                 most-recent-sampling-date timescale-multiplier]}
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
     [typography {:class-name (:input-label classes)} "Locations attribute"]]
    [grid {:item true :xs 6 :xm 6}
     [typography {:class-name (:input-label classes)} "Most recent sampling date"]]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [text-field {:label   "date"
                  :variant :outlined
                  :value   locations-attribute-name}]]
    [grid {:item true
           :xs   6 :xm 6}
     [text-field {:label   "Multiplier"
                  :variant :outlined
                  :value   most-recent-sampling-date}]]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    ;; col
    [grid {:item true :xs 6 :xm 6}
     [typography {:class-name (:input-label classes)} "Time scale"]]]

   [grid {:container true
          :item      true
          :direction :row
          :xs        12 :xm 12}
    [grid {:item true
           :xs   6 :xm 6}
     [text-field {:label   "Multiplier"
                  :variant :outlined
                  :value   timescale-multiplier}]]]])
