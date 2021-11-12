(ns ui.component.app-container
  (:require ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.linear-progress :refer [linear-progress]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent.core :as reagent]
            [shared.components :refer [button collapsible-tab spread-logo]]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.component.search :refer [search-bar]]
            [ui.format :refer [format-percentage]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n type->tab]]))

(def type->label {"CONTINUOUS_TREE"       "Continuous: MCC tree"
                  "DISCRETE_TREE"         "Discrete: MCC tree"
                  "BAYES_FACTOR_ANALYSIS" "Discrete: Bayes Factor Rates"})

(defn completed-menu-item [_]

  (let [menu-open?  (reagent/atom false)
        active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn [{:keys [id readable-name of-type status new?]}]
      (let [badge-text (cond
                         (= status "ERROR") "Error"
                         new?               "New")]
        [:div.completed-menu-item.clickable {:on-click #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id :tab "results"}]
                                                                     (when new?
                                                                       [:graphql/query {:query
                                                                                        "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                                                 touchAnalysis(id: $analysisId) {
                                                                                   id
                                                                                   isNew
                                                                                 }
                                                                               }"
                                                                                        :variables {:analysisId id}}])])}
         [:div.readable-name {:style {:grid-area "readable-name"}} (or readable-name "Unknown")]
         [:div.badges (when badge-text
                        [:span.badge {:class (cond
                                               (= status "ERROR") "error"
                                               new? "new")} badge-text])]
         [:div.sub-name {:style {:grid-area "sub-name"}} (type->label of-type)]
         [:div {:style    {:grid-area "menu"}
                :on-click #(swap! menu-open? not)}
          [:img {:src "icons/icn_kebab_menu.svg"}]]
         (when @menu-open?
           [:ul.menu
            [:li {:on-click #()} "Edit"]
            [:li {:on-click #()} "Load different file"]
            [:li {:on-click #()} "Copy settings"]
            [:li {:on-click (fn [event]
                              (let [{active-route-name :name query :query} @active-page]
                                (.stopPropagation event)

                                ;; if on results page for this analysis we need to nav back to home
                                (when (and (= :route/analysis-results  active-route-name)
                                           (= id (:id query)))
                                  (>evt [:router/navigate :route/home]))

                                (>evt [:graphql/query {:query
                                                       "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                                                   deleteAnalysis(id: $analysisId) {
                                                                     id
                                                                   }
                                                                 }"
                                                       :variables {:analysisId id}}])))}
             "Delete"]])]))))

(defn completed []
  (let [search-term        (re-frame/subscribe [::subs/search])
        completed-analysis (re-frame/subscribe [::subs/completed-analysis-search])
        new-completed      (re-frame/subscribe [::subs/new-completed-analysis])]
    (fn []
      (let [items @completed-analysis
            new-count (count @new-completed)]
        [collapsible-tab (cond-> {:id :completed
                                  :title "Completed data analysis"
                                  :icon "icons/icn_previous_analysis.svg"
                                  :child [:div.completed
                                          [search-bar {:value       (or "" @search-term)
                                                       :on-change   #(>evt [:general/set-search %])
                                                       :placeholder "Search"}]
                                          (for [{:keys [id] :as item} items]
                                            ^{:key id}
                                            [completed-menu-item (-> item)
                                             {}])
                                          ]}
                           (> new-count 0) (assoc :badge-text (str new-count " New")
                                                  :badge-color "purple"))]))))

(defn queued-menu-item [{:keys [readable-name of-type progress]}]
  [:div.queued-menu-item
   [:div.readable-name {:style {:grid-area "readable-name"}} (or readable-name "Unknown")]
   [:div.sub-name {:style {:grid-area "sub-name"}} (type->label of-type)]
   [:div {:style {:grid-area "menu"}} [:img {:src "icons/icn_kebab_menu.svg"}]]
   [:div {:style {:grid-area "play-pause"}} [:img {:src "icons/pause.svg"}]]
   [:div {:style {:grid-area "delete"}} [:img {:src "icons/icn_delete.svg"}]]
   [:div.progress {:style {:grid-area "progress"}}
    [linear-progress {:value      (* 100 progress)
                      :variant    "determinate"}]
    [:span.finished (str (format-percentage progress 1.0) " finished")]]])

(defn queue []
  (let [queued-analysis (re-frame/subscribe [::subs/queued-analysis])]
    (fn []
      (let [items @queued-analysis
            queued-count (count items)]
        [collapsible-tab (cond-> {:id :queued
                                  :title "Queued"
                                  :icon "icons/icn_queue.svg"
                                  :child [:div.queued
                                          (map (fn [{:keys [id] :as item}]
                                                 ^{:key id} [queued-menu-item item])
                                               items)]}
                           (> queued-count 0) (assoc :badge-text (str queued-count " Ongoing")
                                                     :badge-color "grey"))]))))

(defn run-new []
  (let [items [{:main-label "Discrete:"         :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete:"         :sub-label "Bayes factor rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous:"       :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}]]

    [collapsible-tab {:id :run-new
                      :title "Run new analysis"
                      :icon "icons/icn_run_analysis.svg"
                      :child [:div.run-new
                              (map-indexed (fn [index {:keys [main-label sub-label target query]}]
                                             [:li.clickable {:key      index
                                                             :on-click #(>evt [:router/navigate target nil query])}
                                              [:span.label main-label]
                                              [:span.text sub-label]])
                                           items)]}]))

(defn ongoing-menu-item [_]
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn [{:keys [id readable-name of-type]}]
      (let [{:keys [tree-file-name]} @(re-frame/subscribe [::subs/analysis-results id])]
        [:div.ongoing-menu-item.clickable {:on-click
                                           #(>evt [:router/navigate :route/new-analysis nil {:tab (type->tab of-type) :id id}])}
         [:div.readable-name {:style {:grid-area "readable-name"}} (or readable-name tree-file-name "Unknown")]
         [:div.sub-name {:style {:grid-area "sub-name"}} (type->label of-type)]
         [:div {:style {:grid-area "menu"}}
          [icon-button {:size     :small
                        :on-click (fn [event]
                                    (let [{active-route-name :name query :query} @active-page]
                                      (.stopPropagation event)

                                      ;; if on results page for this analysis we need to nav back to home
                                      (when (and (= :route/analysis-results  active-route-name)
                                                 (= id (:id query)))
                                        (>evt [:router/navigate :route/home]))

                                      (>evt [:graphql/query {:query
                                                             "mutation DeleteAnalysisMutation($analysisId: ID!) {
                                                                   deleteAnalysis(id: $analysisId) {
                                                                     id
                                                                   }
                                                                 }"
                                                             :variables {:analysisId id}}])))}
           [avatar {:alt     "delete"
                    :variant "square"
                    :src     (arg->icon (:delete icons))}]]]]))))

(defn ongoing []
  (let [ongoing-analysis (re-frame/subscribe [::subs/ongoing-analysis])]
    (fn []
      (let [items         @ongoing-analysis
            ongoing-count (count @ongoing-analysis)]
        [collapsible-tab (cond-> {:id :ongoing
                                  :title "Ongoing data analysis"
                                  :icon "icons/icn_upload.svg"
                                  :child [:div.ongoing
                                          (for [{:keys [id] :as item} items]
                                            ^{:key id}
                                            [ongoing-menu-item item])]}
                           (> ongoing-count 0) (assoc :badge-text (str ongoing-count " Ongoing")
                                                      :badge-color "purple"))]))))

(defn main-menu []
  [:div.app-sidebar.panel
   [:div.collapsible-tabs
    [run-new]
    [ongoing]
    [completed]
    [queue]]
   [:div.footer
    [button {:text "Run new analysis"
             :on-click #(>evt [:router/navigate :route/new-analysis nil {:tab "continuous-mcc-tree"}])
             :class "primary"
             :icon "icons/icn_run_analysis_white.svg"}]]])

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
                              (>evt [:graphql/query {:query
                                                     "mutation DeleteUserDataMutation {
                                                                 deleteUserData {
                                                                   userId
                                                                 }
                                                               }"}])
                              (handle-close))}
       "Clear data"]
      [menu-item {:on-click (fn []
                              (>evt [:graphql/query {:query
                                                     "mutation DeleteUserAccountMutation {
                                                                 deleteUserAccount {
                                                                   userId
                                                                 }
                                                               }"}])

                              (handle-close))}
       "Delete account"]]]))

(defn header-logo []
  [:div.app-header-logo {:on-click #(>evt [:router/navigate :route/home])}
   [spread-logo]])

(defn header-menu []
  [:div.app-header-menu
   [user-login {}]])

(defn app-container []
  (fn [child-page]
    [:div.app-container-grid
     [:div.app-header-spacer-1]
     [header-logo]
     [header-menu]
     [:div.app-header-spacer-2]
     [main-menu]
     [:div.app-body.panel
      child-page]]))
