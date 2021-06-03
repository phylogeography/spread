(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ui.component.button
             :refer
             [button-with-icon button-with-icon-and-label]]
            [ui.component.icon :refer [icon-with-label icons]]
            [ui.format :refer [format-percentage]]
            [ui.component.indicator :refer [busy loading]]
            [ui.subscriptions :as subs]
            [ui.utils :as ui-utils :refer [>evt]]
            ["react-infinite-scroll-component" :as InfiniteScroll]))

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
         [icon-with-label {:icon (:spread icons)
                           :label "spread"
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
    (fn [{:keys [id readable-name of-type]}]
      [:div.completed-menu-item {:on-click #(re-frame/dispatch [:router/navigate :route/analysis-results nil {:id id}])}
       [:div
        [:span readable-name]
        ;; (when-not seen? [:span "New"])
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
        [:div of-type]]])))

;; TODO
;; https://github.com/ankeetmaini/react-infinite-scroll-component#readme
;; TODO : searching
(defn completed [{:keys [open?]}]
  (let [;; TODO : gql search query by readable-name
        search-text (reagent/atom "")
        open?       (reagent/atom open?)
        edges       (re-frame/subscribe [::subs/user-analysis-edges])
        page-info   (re-frame/subscribe [::subs/user-analysis-page-info])
        next        (fn [end-cursor]

                      (prn "@ next" end-cursor)

                      (>evt [:graphql/query {:query
                                             "query SearchAnalysis($endCursor: String!) {
                                                searchUserAnalysis(first: 3, after: $endCursor, statuses: [SUCCEEDED]) {
                                                pageInfo {
                                                  hasNextPage
                                                  startCursor
                                                  endCursor
                                                }
                                                edges {
                                                  cursor
                                                  node {
                                                    id
                                                    readableName
                                                    ofType
                                                    status
                                                    createdOn
                                                  }
                                                }
                                              }
                                            }"
                                             :variables {:endCursor end-cursor}}]))]
    (fn []
      (let [{:keys [has-next-page end-cursor]} @page-info]

        (prn "@ page info " has-next-page end-cursor (count @edges))

        [:div.completed {:on-click #(swap! open? not)
                         :class    (when @open? "open")}
         [:div
          [:img {:src (:completed icons)}]
          [:span "Completed data analysis"]
          [:img {:src (:dropdown icons)}]]
         [:input.search-input {:value       @search-text
                               :on-change   #(reset! search-text (-> % .-target .-value))
                               :type        "text"
                               :placeholder "Search..."}]
         [:> InfiniteScroll {:dataLength       (count @edges)
                             :height           200
                             :hasMore          has-next-page
                             :next             #(next end-cursor)}
          (doall
            (map (fn [{:keys [cursor node]}]
                   ^{:key cursor} [completed-menu-item node])
                 @edges))]]))))

(defn queue-menu-item []
  (let [menu-opened? (reagent/atom false)]
    (fn [{:keys [id readable-name of-type progress]}]
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

;; TODO : subscribe to status updates for all ongoing
(defn queue [{:keys [open?]}]
  (let [open?         (reagent/atom open?)
        total-ongoing 2
        data          [{:id            "4"
                        :readable-name "Relaxed_dollo_AllSingleton_v1"
                        :progress      0.8
                        :of-type       "Continuous: MCC Tree"}
                       {:id            "5"
                        :readable-name "Relaxed_dollo_AllSingleton_v3"
                        :progress      0.3
                        :of-type       "Continuous: Time slices"}]]
    (fn []
      [:div.queue {:on-click #(swap! open? not)
                   :class    (when @open? "open")}
       [:div
        [:img {:src (:queue icons)}]
        [:span "Queue"]
        [:span.notification (str total-ongoing " Ongoing")]
        [:img {:src (:dropdown icons)}]]
       [:ul.menu-items
        (doall
          (map (fn [{:keys [id] :as item}]
                 [:li.menu-item {:key id}
                  [queue-menu-item item]])
               data))]])))

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
                                  :on-click #(re-frame/dispatch [:router/navigate :route/new-analysis])}]]))

(defn app-container []
  (fn [child-page]
    [:div.grid-container
     [:div.app-header
      [header]]
     [main-menu]
     [:div.main
      child-page]]))
