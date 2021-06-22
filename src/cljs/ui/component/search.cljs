(ns ui.component.search
  (:require [reagent.core :as reagent]
            ["material-ui-search-bar" :as SearchBar]))

(def search-bar (reagent/adapt-react-class (.-default SearchBar)))
