(ns ui.component.search
  (:require ["material-ui-search-bar" :as SearchBar]
            [reagent.core :as reagent]))

(def search-bar (reagent/adapt-react-class (.-default SearchBar)))
