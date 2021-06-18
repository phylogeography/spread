(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ui.component.button
             :refer
             [button-with-icon button-with-icon-and-label]]
            [ui.component.icon :refer [icon-with-label icons]]
            [ui.format :refer [format-percentage]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt dispatch-n]]))

(defn user-login [email]
  [:div.hover-dropdown
   [:div
    [:span email]
    [:img {:src (:user icons)}]
    [:img {:src (:dropdown icons)}]]
   [:div.dropdown-content
    [:a {:on-click #(>evt [:general/logout])} "Log out"]
    [:a {:on-click #(prn "TODO: clear-data")} "Clear data"]
    [:a {:on-click #(prn "TODO: delete-account")} "Delete account"]]])

(defn header []
  (let [authed-user (re-frame/subscribe [::subs/authorized-user])]
    (fn []
      (let [{:keys [email]} @authed-user]
        [:div.header
         [icon-with-label {:icon     (:spread icons)
                           :label    "spread"
                           :on-click #(re-frame/dispatch [:router/navigate :route/home])}]
         [user-login email]]))))

(defn run-new [{:keys [open?]}]
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
    (fn [{:keys [id readable-name of-type status new?] :as args}]
      (let [error? (= "ERROR" status)]
        ;; TODO dispatch touch mutation
        [:div.completed-menu-item {:on-click #(dispatch-n [[:router/navigate :route/analysis-results nil {:id id}]
                                                           [:graphql/query {:query
                                                                            "mutation TouchAnalysisMutation($analysisId: ID!) {
                                                                                        touchAnalysis(id: $analysisId) {
                                                                                          id
                                                                                          isNew
                                                                                        }
                                                                                      }"
                                                                            :variables {:analysisId id}}]])}
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

(defn completed [{:keys [open?]}]
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

(defn queued-menu-item []
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

(defn queue [{:keys [open?]}]
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
(defn main-menu []
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

(defn app-container []
  (fn [child-page]
    [:div.grid-container
     [:div.app-header
      [header]]
     [main-menu]
     [:div.main
      child-page]]))
