(ns ui.component.fixed-size-list
  (:require [reagent.core :as reagent]
            ["react-window" :refer (FixedSizeList)]))

(def fixed-size-list (reagent/adapt-react-class FixedSizeList))
