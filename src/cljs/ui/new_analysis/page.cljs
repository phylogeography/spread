(ns ui.new-analysis.page
  (:require [re-frame.core :as re-frame]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [ui.component.app-container :refer [app-container]]
            [ui.new-analysis.continuous-mcc-tree :refer [continuous-mcc-tree]]
            [ui.new-analysis.discrete-mcc-tree :refer [discrete-mcc-tree]]
            [ui.new-analysis.discrete-rates :refer [discrete-rates]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.utils :as ui-utils :refer [>evt]]))

(def use-styles (styles/make-styles (fn [theme]
                                      {:centered {:display         :flex
                                                  :justify-content :center
                                                  :align-items     :center}

                                       :header {:font  "normal normal 900 24px/28px Roboto"
                                                :color "#3A3668"}

                                       :app-bar {:background    "#FFFFFF"
                                                 :box-shadow    :none
                                                 :border-bottom "1px solid #DEDEE7"}

                                       :tab-title {:text-transform :none
                                                   :text-align     :center
                                                   :font           "normal normal medium 16px/19px Roboto"
                                                   :color          "#3A3668"}

                                       :tab-subtitle {:text-transform :none
                                                      :text-align     :center
                                                      :font           "normal normal 900 10px/11px Roboto"
                                                      :letter-spacing "0px"
                                                      :color          "#757295"}

                                       :indicator {:background       "#EEBE53"
                                                   :background-color "#EEBE53"}

                                       :upload-button {:textTransform  "none"
                                                       :font           "normal normal medium 16px/19px Roboto"
                                                       :letter-spacing "0px"
                                                       :background     "#3428CA"
                                                       :color          "#ECEFF8"
                                                       :border-radius  "8px"
                                                       :width          "324px"
                                                       :height         "48px"}

                                       :upload-progress {:border-radius "8px"
                                                         :width         "324px"
                                                         :height        "48px"}

                                       :border {:border        "1px solid #DEDEE7"
                                                :display       :flex
                                                :align-items   :center
                                                :width         "373px"
                                                :height        "46px"
                                                :border-radius "8px"}

                                       :icon-button {:width  "14px"
                                                     :height "18px"}

                                       :input-label {:font        "normal normal medium 16px/19px Roboto"
                                                     :color       "#3A3668"
                                                     :font-weight :bold}

                                       :outlined-input {:height "46px"
                                                        :font   "normal normal medium 14px/16px Roboto"
                                                        :color  "#3A3668"}

                                       :form-control {:margin    ((:spacing theme) 1)
                                                      :min-width 120}

                                       :date-picker {:border-radius "8px"
                                                     :border        "1px solid #E2E2EA"}

                                       :start-button {:background     "EEBE53"
                                                      :box-shadow     "0px 10px 30px #EEBE5327"
                                                      :border-radius  "8px"
                                                      :font           "normal normal medium 16px/19px Roboto"
                                                      :color          "#3A3668"
                                                      :text-transform :none}

                                       :slider {:width 324}})))



;; NOTE: specs
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/db6d1f78-c5f4-460e-9f97-32e9df007388/specs/
;; https://app.zeplin.io/project/6075ecb45aa2eb47e1384d0b/screen/6075ed2b39f8103c14a9c660
(defmethod page :route/new-analysis []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [query]}   @active-page
            {active-tab :tab} query
            classes           (use-styles)]
        [app-container
         [grid
          [app-bar {:position   "static"
                    :color      :transparent
                    :class-name (:app-bar classes)}
           [toolbar {:class-name (:centered classes)}
            [typography {:class-name (:header classes)} "Run new analysis"]]]
          [tabs {:value     active-tab
                 :centered  true
                 :classes   {:indicator (:indicator classes)}
                 :on-change (fn [_ value]
                              (>evt [:router/navigate :route/new-analysis nil {:tab value}]))}
           [tab {:value "discrete-mcc-tree"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Discrete"]
                           [typography {:class-name (:tab-subtitle classes)} "MCC tree"]])}]
           [tab {:value "discrete-rates"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Discrete"]
                           [typography {:class-name (:tab-subtitle classes)} "Rates"]])}]
           [tab {:value "continuous-mcc-tree"
                 :label (reagent/as-element
                          [:div
                           [typography {:class-name (:tab-title classes)} "Continuous"]
                           [typography {:class-name (:tab-subtitle classes)} "MCC tree"]])}]]
          (case active-tab
            "discrete-mcc-tree"   [discrete-mcc-tree classes]
            "discrete-rates"      [discrete-rates classes]
            "continuous-mcc-tree" [continuous-mcc-tree classes]
            [continuous-mcc-tree classes])]]))))
