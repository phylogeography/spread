(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ui.component.button
             :refer
             [button-with-icon button-with-icon-and-label]]
            [ui.component.icon :refer [icon-with-label icons]]
            [ui.format :refer [format-percentage]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]
            [ui.component.icon :refer [arg->icon icons]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.button :refer [button]]
            ;; [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]

            [reagent-material-ui.core.accordion :refer [accordion]]
            [reagent-material-ui.core.accordion-summary :refer [accordion-summary]]
            [reagent-material-ui.core.accordion-details :refer [accordion-details]]

            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-avatar :refer [list-item-avatar]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]

            [reagent-material-ui.core.box :refer [box]]

            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-header :refer [card-header]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            ["react" :as react]
            [ui.component.fixed-size-list :refer [fixed-size-list]]
            ))





#_(defn queued-menu-item []
  (let [;; TODO : use css on-hover (or get rid of it entirely)
        menu-opened? (reagent/atom false)
        ]
    (fn [{:keys [id readable-name of-type
                 #_status
                 progress]
          :or   {readable-name "Unknown"}}]
      [:div.queue-menu-item
       {:on-click #(re-frame/dispatch [:router/navigate :route/analysis-results nil {:id id}])}
       [:div
        [:span readable-name]
        [:div.click-dropdown
         [button-with-icon {:on-click #(swap! menu-opened? not)
                            :icon     (:kebab-menu icons)}]
         [:div.dropdown-content {:class (when @menu-opened? "dropdown-menu-opened")}
          [:a {:on-click (fn [event]
                           (prn "TODO: Edit")
                           (.stopPropagation event))} "Edit"]
          [:a {:on-click (fn [event]
                           (prn "TODO: Load")
                           (.stopPropagation event))} "Load different file"]
          [:a {:on-click (fn [event]
                           (prn "TODO: Copy")
                           (.stopPropagation event))} "Copy settings"]
          [:a {:on-click (fn [event]
                           (prn "TODO: Show delete modal")
                           (.stopPropagation event))} "Delete"]]]
        [:div of-type]]
       [:div
        [:div
         [:progress {:max 1 :value progress}]
         [button-with-icon {:on-click #(prn "TODO: delete ongoing analysis")
                            :icon     (:delete icons)}]]
        [:span (str (format-percentage progress 1.0) " finished")]]])))

#_(defn queue [{:keys [open?]}]
  (let [open?           (reagent/atom open?)
        queued-analysis (re-frame/subscribe [::subs/queued-analysis])]
    (fn []
      [:div.queue {:on-click #(swap! open? not)
                   :class    (when @open? "open")}
       [:div
        [:img {:src (:queue icons)}]
        [:span "Queue"]
        [:span.notification (str (count @queued-analysis) " Ongoing")]
        [:img {:src (:dropdown icons)}]]
       [:div.menu-items.scrollable-area
        (doall
          (map (fn [{:keys [id] :as item}]
                 ^{:key id}
                 [queued-menu-item item])
               @queued-analysis))]])))

(def use-styles (styles/make-styles (fn [theme]
                                      {
                                       :root {
                                              :background "#ECEFF8 0% 0% no-repeat padding-box"
                                              :min-width  "100%"
                                              :min-height "100vh"
                                              ;; :flex-grow 1
                                              }

                                       :card {:box-shadow    "0px 30px 60px #313B5833"
                                              :border-radius "20px"}

                                       :centered {:display         "flex"
                                                  :flex-direction  "row"
                                                  :justify-content "center"}

                                       :email {:font "normal normal medium 16px/19px Roboto"
                                               :letter-spacing "0px"
                                               :color "#3A3668"
                                               }

                                       :app-bar {
                                                 :background "#FFFFFF 0% 0% no-repeat padding-box"
                                                 ;; :border     "none"
                                                 :box-shadow :none
                                                 :border-bottom     "1px solid #DEDEE7"
                                                 :margin-bottom 30
                                                 }

                                       :header {:display "flex"
                                                :flex-direction "row"
                                                :justify-content "space-between"
                                                ;; :align-items "center"
                                                }

                                       :title {
                                               :flexGrow 1
                                               :text-align     "left"
                                               :font           "normal normal 900 30px/35px Roboto"
                                               :letter-spacing "3.9px"
                                               :color          "#3A3668"
                                               :text-transform "uppercase"
                                               }

                                       :heading {
                                                 :text-align "left"
                                                 :font "normal normal medium 16px/19px Roboto"
                                                 :letter-spacing "0px"
                                                 :color "#3A3668"
                                                 :font-weight :bold
                                                 }

                                       :box {:padding-right 5
                                             :text-align "left"
                                             :font "normal normal medium 16px/19px Roboto"
                                             :font-weight :bold
                                             :letter-spacing "0px"
                                             :color "#3A3668"}

                                       :list-item-text {:color "#757295"}

                                       :button {:textTransform  "none"
                                                :font "normal normal medium 16px/19px Roboto"
                                                :letter-spacing "0px"
                                                :background "#3428CA 0% 0% no-repeat padding-box"
                                                :color "#ECEFF8"}

                                       })))


(defn completed-menu-item []
  #_(let [menu-opened? (reagent/atom false)]
    (fn [{:keys [id readable-name of-type status new?]}]
      (let [error? (= "ERROR" status)]
        ;; TODO dispatch touch mutation
        [:div.completed-menu-item {:on-click #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id}]
                                                           (when new?
                                                             [:graphql/query {:query
                                                                              "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                                                        touchAnalysis(id: $analysisId) {
                                                                                          id
                                                                                          isNew
                                                                                        }
                                                                                      }"
                                                                              :variables {:analysisId id}}])])}
         [:div
          [:span (or readable-name "Unknown")]
          (when new? [:span "New"])
          (when error? [:span "Error"])
          [:div.click-dropdown
           [button-with-icon {:on-click #(swap! menu-opened? not)
                              :icon     (:kebab-menu icons)}]
           ;; TODO : with css on-hover
           [:div.dropdown-content {:class (when @menu-opened? "dropdown-menu-opened")}
            [:a {:on-click (fn [event]
                             (prn "TODO: Edit")
                             (.stopPropagation event))} "Edit"]
            [:a {:on-click (fn [event]
                             (prn "TODO: Load")
                             (.stopPropagation event))} "Load different file"]
            [:a {:on-click (fn [event]
                             (prn "TODO: Copy")
                             (.stopPropagation event))} "Copy settings"]
            [:a {:on-click (fn [event]
                             (prn "TODO: Show delete modal")
                             (.stopPropagation event))} "Delete"]]]
          [:div of-type]]]))))

;; https://material-ui.com/components/lists/#virtualized-list
(defn completed [classes]
  (let [search-term        (re-frame/subscribe [::subs/search])
        completed-analysis (re-frame/subscribe [::subs/completed-analysis-search])]
    (fn []
      (let [items @completed-analysis]

        ;; (prn "@ Items" items)

        [accordion {:defaultExpanded true}
         [accordion-summary {:expand-icon   (reagent/as-element [:img {:src (:dropdown icons)}])}
          [:img {:src (:completed icons)}]
          [typography {:class-name (:heading classes)} "Completed data analysis"]]
         [divider {:variant "fullWidth"}]
         [accordion-details
          [list
           (doall
             (map-indexed (fn [index {:keys [id readable-name of-type status new?] :as item}]
                            [list-item {:key      index
                                        :button   false #_true
                                        :on-click
                                        #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id}]
                                                      (when new?
                                                        [:graphql/query {:query
                                                                         "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                                                        touchAnalysis(id: $analysisId) {
                                                                                          id
                                                                                          isNew
                                                                                        }
                                                                                      }"
                                                                         :variables {:analysisId id}}])])}

                             [list-item-text {:primary (or readable-name "Unknown") :secondary of-type}]
                             ;; list-item-avatar
                             ;;avatar
                             [:img {:src (:kebab-menu icons)}]

      ;; <ListItem>
      ;;   <ListItemAvatar>
      ;;     <Avatar>
      ;;       <ImageIcon />
      ;;     </Avatar>
      ;;   </ListItemAvatar>
      ;;   <ListItemText primary="Photos" secondary="Jan 9, 2014" />
      ;; </ListItem>

                             ;; [:div id]
                             ;; TODO


                             ])
                          items))]]]))))

(defn run-new [classes]
  (let [items [{:main-label "Discrete:"         :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete:"         :sub-label "Rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous:"       :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}]]
    [accordion {:defaultExpanded true}
     [accordion-summary {:expand-icon (reagent/as-element [:img {:src (:dropdown icons)}])}
      [:img {:src (:run-new icons)}]
      [typography {:class-name (:heading classes)} "Run new analysis"]]
     [divider {:variant "fullWidth"}]
     [accordion-details
      [list
       (doall
         (map-indexed (fn [index {:keys [main-label sub-label target query]}]
                        [list-item {:key      index
                                    :button   true
                                    :on-click #(re-frame/dispatch [:router/navigate target nil query])}
                         [box {:class-name (:box classes)}
                          main-label]
                         [list-item-text
                          {:class-name               (:list-item-text classes)
                           :secondary                sub-label
                           :secondaryTypographyProps {:align "left"}}]])
                      items))]]]))

(defn main-menu [classes]
  [:div
   [run-new classes]
   [completed classes]
   ;; TODO

   [button {:variant   "contained"
            :color     "primary"
            :size      "large"
            :className (:button classes)
            :startIcon (reagent/as-element [:img {:src (:run-new icons)}])
            :on-click  #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])}
    "Run new analysis"]])

(defn user-login [classes]
  (let [{:keys [email]}                  @(re-frame/subscribe [::subs/authorized-user])
        [anchorElement setAnchorElement] (react/useState nil)
        handle-close                     #(setAnchorElement nil)
        open?                            (not (nil? anchorElement))]
    [:div
     [icon-button {:aria-label    "authed user menu"
                   :aria-controls "menu-appbar"
                   :aria-haspopup true
                   :color         "inherit"
                   :onClick       (fn [^js event]
                                    (setAnchorElement (.-currentTarget event)))}
      [typography {:class-name (:email classes)} email]
      [:img {:src (:user icons)}]
      [:img {:src (:dropdown icons)}]]
     [menu {:id               "menu-appbar"
            :anchorEl         anchorElement
            :anchorOrigin     {:vertical   "top"
                               :horizontal "right"}
            :keep-mounted     true
            :transform-origin {:vertical   "top"
                               :horizontal "right"}
            :open             open?
            :on-close         handle-close}
      [menu-item {:on-click (fn []
                              (handle-close)
                              (>evt [:general/logout]))} "Log out"]
      [menu-item {:on-click (fn []
                              (prn "TODO")
                              (handle-close))}
       "Clear data"]
      [menu-item {:on-click (fn []
                              (prn "TODO")
                              (handle-close))}
       "Delete account"]]]))

(defn header [classes]
  [app-bar {:position   "static"
            :class-name (:app-bar classes)}
   [grid {:container true
          :spacing   2}
    [grid {:item true :xs false :sm 2 } #_"left gutter"]
    [grid {:item true :xs 8 :sm 8}
     [toolbar {:disableGutters true}
      [icon-button {:class-name (:menu-button classes) :edge "start" :color "inherit" :aria-label "menu"}
       [avatar {:alt "spread" :variant "square" :src (arg->icon (:spread icons))}]]
      [typography {:class-name (:title classes) :variant "h6" } "Spread"]
      [user-login classes]]]
    [grid {:item true :xs false :sm 2 } #_"right gutter"]]])

(defn app-container []
  (let [classes (use-styles)]
    (fn [child-page]
      [grid {:class-name  (:root classes)
             :container true
             :direction "column"}
       [header classes]
       [grid {:container true
              :spacing   2}
        [grid {:item true :xs false :sm 2 } #_"left gutter"]
        [grid {:item true :xs 2 :sm 2}
         [card {:class-name (:card classes)}
          [card-content #_{:styles {:padding 0
                                  "&:last-child" {
                                    :paddingBottom 0}
                                  }}
           [main-menu classes]]]]
        [grid {:item true :xs 6 :sm 6}
         [card {:class-name (:card classes)}
          [card-content
           child-page]]]
        [grid {:item true :xs false :sm 2 } #_"right gutter"]]])))
