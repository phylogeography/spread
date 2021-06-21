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
            ))


#_(defn header []
    (let [authed-user (re-frame/subscribe [::subs/authorized-user])]
      (fn []
        (let [{:keys [email]} @authed-user]
          [:div.header
           [icon-with-label {:icon     (:spread icons)
                             :label    "spread"
                             :on-click #(re-frame/dispatch [:router/navigate :route/home])}]
           [user-login email]]))))

#_(defn run-new [{:keys [open?]}]
  (let [open? (reagent/atom open?)
        items [{:main-label "Discrete"          :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete"          :sub-label "Rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous"        :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}]]
    (fn []
      [:div.run-new {:on-click #(swap! open? not)
                     :class    (when @open? "open")}
       [:div
        [:img {:src (:run-analysis icons)}]
        [:span "Run new analysis"]
        [:img {:src (:dropdown icons)}]]
       [:ul
        (doall
          (map-indexed (fn [index {:keys [main-label sub-label target query]}]
                         [:li.run-new-analysis-menu-item {:key index}
                          [:div {:on-click #(re-frame/dispatch [:router/navigate target nil query])}
                           [:a [:span [:b (str main-label ":")] sub-label]]]])
                       items))]])))

(defn completed-menu-item []
  (let [menu-opened? (reagent/atom false)]
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

#_(defn completed [{:keys [open?]}]
  (let [search-term        (re-frame/subscribe [::subs/search])
        ;; TODO : achieve it with CSS
        open?              (reagent/atom open?)
        completed-analysis (re-frame/subscribe [::subs/completed-analysis-search])]
    (fn []
      [:div.completed {:on-click #(swap! open? not)
                       :class    (when @open? "open")}
       [:div
        [:img {:src (:completed icons)}]
        [:span "Completed data analysis"]
        [:img {:src (:dropdown icons)}]]
       [:input.search-input {:value       @search-term
                             :on-change   #(>evt [:general/set-search (-> % .-target .-value)])
                             :type        "text"
                             :placeholder "Search..."}]
       [:div.menu-items.scrollable-area
        (doall
          (map (fn [{:keys [id] :as item}]
                 ^{:key id}
                 [completed-menu-item item])
               @completed-analysis))]])))

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

;; TODO : CSS for open / close
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/bfa17d6e-7b48-4547-8af8-b975b452dd35/
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/44bb1ba7-e9f8-4752-95da-942f04ea32d2/specs/
#_(defn main-menu []
    (fn []
      [:div.main-menu
       [:ul.main-menu-navigation
        [:li.nav-item
         [run-new {:open? true}]]
        [:li.nav-item
         [completed {:open? false}]]
        [:li.nav-item
         [queue {:open? false}]]]
       [button-with-icon-and-label {:class    "analysis-button"
                                    :icon     (:run-analysis icons)
                                    :label    "Run new analysis"
                                    :on-click #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])}]]))

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
                                               ;; :opacity 1
                                               }


                                       :app-bar {
                                                 :background "#FFFFFF 0% 0% no-repeat padding-box"
                                                 ;; :border     "none"
                                                 :box-shadow :none
                                                 :border-bottom     "1px solid #DEDEE7"
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

                                       })))

;; https://github.com/AtotheY/material-ui-ux-video

(defn main-menu []

  [typography "MENU"]

    )

;; https://codesandbox.io/s/yr7fc?file=/demo.js
(defn user-login [classes]
  (let [{:keys [email]} @(re-frame/subscribe [::subs/authorized-user])
        [anchorElement setAnchorElement] (react/useState nil)
        handle-close (fn []

                       (prn "@ handel-close" {:anchorElement anchorElement
                                              :setAnchorElement setAnchorElement})

                       (setAnchorElement nil))
        open? (not (nil? anchorElement))]

    (prn "@ render" {:anchorElement    anchorElement
                     :setAnchorElement setAnchorElement
                     :open?            open?})

    ;; <div>
    ;;    <IconButton
    ;;      aria-label="account of current user"
    ;;      aria-controls="menu-appbar"
    ;;      aria-haspopup="true"
    ;;      onClick={handleMenu}
    ;;      color="inherit"
    ;;    >
    ;;      <AccountCircle />
    ;;    </IconButton>
    ;;    <Menu
    ;;      id="menu-appbar"
    ;;      anchorEl={anchorEl}
    ;;      anchorOrigin={{
    ;;        vertical: 'top',
    ;;        horizontal: 'right',
    ;;      }}
    ;;      keepMounted
    ;;      transformOrigin={{
    ;;        vertical: 'top',
    ;;        horizontal: 'right',
    ;;      }}
    ;;      open={open}
    ;;      onClose={handleClose}
    ;;    >
    ;;      <MenuItem onClick={handleClose}>Profile</MenuItem>
    ;;      <MenuItem onClick={handleClose}>My account</MenuItem>
    ;;    </Menu>
    ;;  </div>

    [:div
     [icon-button {
                   :aria-label    "authed user menu"
                   :aria-controls "menu-appbar"
                   :aria-haspopup true
                   :color         "inherit"
                   :onClick       (fn [^js event]
                                    (prn "click!" (.-currentTarget event))
                                    (setAnchorElement (.-currentTarget event)))}
      [typography {:class-name (:email classes)} email]
      [:img {:src (:user icons)}]
      [:img {:src (:dropdown icons)}]]

     [menu {:id           "menu-appbar"
            :anchorEl     anchorElement
            :anchorOrigin {:vertical   "top"
                           :horizontal "right"}
            :keep-mounted     true
            :transform-origin {:vertical   "top"
                               :horizontal "right"}
            :open         open?
            :on-close     handle-close
            }

      [menu-item {:on-click (fn []
                              (handle-close)
                              #_(>evt [:general/logout]))} "Log out"]
      [menu-item {:on-click (fn []
                              (handle-close))} "Clear data"]
      [menu-item {:on-click (fn []
                              (handle-close))} "Delete account"]

      ]
     ]

    ))

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
      [:f> user-login classes]]]
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
          [card-content
           [main-menu]]]]
        [grid {:item true :xs 6 :sm 6}
         [card {:class-name (:card classes)}
          [card-content
           child-page]]]
        [grid {:item true :xs false :sm 2 } #_"right gutter"]]])))
