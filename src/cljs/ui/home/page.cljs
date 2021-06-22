(ns ui.home.page
  (:require [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.app-container :refer [app-container]]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.router.component :refer [page]]
            [ui.utils :as ui-utils :refer [>evt]]))

(def use-styles (styles/make-styles (fn [theme]
                                      {:centered {:display         :flex
                                                  :justify-content :center
                                                  :align-items     :center}

                                       :title {:font           "normal normal 900 30px/35px Roboto"
                                               :letter-spacing "3.9px"
                                               :color          "#3A3668"
                                               :text-transform "uppercase"}

                                       :button {:textTransform  "none"
                                                :font           "normal normal medium 16px/19px Roboto"
                                                :letter-spacing "0px"
                                                :background     "#3428CA 0% 0% no-repeat padding-box"
                                                :color          "#ECEFF8"
                                                :border-radius  "8px"
                                                :width          325}

                                       :button-documentation {:textTransform "none"
                                                              :border-radius "8px"}

                                       :welcome {:text-align  :center
                                                 :font        "normal normal 900 24px/28px Roboto"
                                                 :font-weight :bold
                                                 :color       "#3A3668"}

                                       :paragraph {:text-align :left
                                                   :font       "normal normal medium 16px/30px Roboto"
                                                   :color      "#757295"}

                                       :divider {:width 325}})))

(defmethod page :route/home []
  (fn []
    (let [classes (use-styles)]
      [app-container
       [grid {:container   true
              :direction   :column
              :align-items :center}
        [box {:paddingBottom 10}
         [grid {:container  true
                :direction  :row
                :item       true
                :class-name (:centered classes)}
          [avatar {:alt "spread" :variant "square" :src (arg->icon (:spread icons))}]
          [typography {:class-name (:title classes) :variant "h6"} "Spread"]]]
        [box {:paddingBottom 3}
         [typography {:class-name (:welcome classes)} "Welcome to Spread."]]
        [box {:paddingBottom 3}
         [typography {:class-name (:paragraph classes)} "System for visualizing phylogeographic reconstructions."]]
        [box {:paddingBottom 3}
         [typography {:class-name (:paragraph classes)} "Click on the button below and follow the given directions."]]
        [button {:variant   "contained"
                 :color     "primary"
                 :size      "large"
                 :className (:button classes)
                 :startIcon (reagent/as-element [:img {:src (:run-new icons)}])
                 :on-click  #(>evt [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])}
         "Run new analysis"]
        [box {:padding 10}
         [divider {:variant    "fullWidth"
                   :class-name (:divider classes)}]]
        [box {:paddingBottom 3}
         [typography {:class-name (:paragraph classes)} "Check the documentation for further informations."]]
        [box {:paddingBottom 10}
         [button {:variant   "outlined"
                  :color     "primary"
                  :size      "large"
                  :className (:button-documentation classes)
                  :on-click  #(>evt [:router/navigate :route/documentation])}
          "Read documentation"]]]])))
