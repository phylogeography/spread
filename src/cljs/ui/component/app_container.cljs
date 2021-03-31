(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [reagent.core :as reagent]
            [ui.component.icon :refer [icons icon-with-label]]
            [taoensso.timbre :as log]))

(defn user-login []
  ;;let [opened? (reagent/atom false)]
  (fn [{:keys [email]}]
    [:div.hover-dropdown
     [:div [:span email] [:img {:src (:user icons)}] [:img {:src (:dropdown icons)}]]
     [:div.dropdown-content
      [:a {:on-click #(prn "TODO: logout")} "Log out"]
      [:a {:on-click #(prn "TODO: clear-data")} "Clear data"]
      [:a {:on-click #(prn "TODO: delete-account")} "Delete account"]]]))

(defn header []
  (let [authed-user (re-frame/subscribe [::subs/authorized-user])]
    (fn []
      (let [{:keys [email]} @authed-user]
        [:div.header
         [icon-with-label {:icon (:spread icons) :label "spread"}]
         [user-login {:email email}]]))))

(defn run-new [{:keys [open?]}]
  (let [open? (reagent/atom open?)
        items [{:main-label "Discrete"          :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "discrete-mcc-tree"}}
               {:main-label "Discrete"          :sub-label "Rates"
                :target     :route/new-analysis :query     {:tab "discrete-rates"}}
               {:main-label "Continuous"        :sub-label "MCC tree"
                :target     :route/new-analysis :query     {:tab "continuous-mcc-tree"}}
               {:main-label "Continuous"        :sub-label "Time slices"
                :target     :route/new-analysis :query     {:tab "continuous-time-slices"}}]]
    (fn []
      [:div.run-new {:on-click #(swap! open? not)
                     :class    (when @open? "open")}
       [:a [:img {:src (:run-analysis icons)}] "Run new analysis" [:img {:src (:dropdown icons)}]]
       [:ul
        (doall
          (map-indexed (fn [index {:keys [main-label sub-label target query] :as item}]
                         [:li.menu-item {:key index} [:a.run-new-analysis-menu-item {:on-click #(re-frame/dispatch [:router/navigate target nil query])}
                                                      [:span [:b (str main-label ":")] sub-label]]])
                       items))]])))

(defn completed-menu-item []
  (let [opened? (reagent/atom false)]
    (fn [{:keys [id readable-name of-type seen?]}]
      [:div.completed-menu-item {:on-click #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab (case of-type
                                                                                                                 "Continuous: MCC Tree"    "continuous-mcc-tree"
                                                                                                                 "Continuous: Time slices" "continuous-time-slices"
                                                                                                                 "Discrete: MCC Tree"      "discrete-mcc-tree"
                                                                                                                 "Discrete: Rates"         "discrete-rates"
                                                                                                                 nil)
                                                                                                          :id  id}])}
       [:div readable-name
        (when-not seen? [:div "New"])
        [:div.click-dropdown
         [:button {:on-click (fn [event]
                               (swap! opened? not)
                               (.stopPropagation event))}
          [:img {:src (:kebab-menu icons)}]]
         [:div.dropdown-content {:class (when @opened? "dropdown-menu-opened")}
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
                           (.stopPropagation event))} "Delete"]]]]
       [:div of-type]])))

;; TODO : gql search query : completed by readable-name
(defn completed [{:keys [open?]}]
  (let [search-text  (reagent/atom "")
        open?        (reagent/atom open?)
        total-unseen 1
        data         [{:id            "1"
                       :readable-name "Relaxed_dollo_AllSingleton_v2"
                       :seen?         false
                       :of-type       "Continuous: MCC Tree"}
                      {:id            "2"
                       :readable-name "Relaxed_dollo_AllSingleton_v2"
                       :seen?         true
                       :of-type       "Continuous: Time slices"}
                      {:id            "3"
                       :readable-name "Relaxed_dollo_AllSingleton_v2"
                       :seen?         true
                       :of-type       "Discrete: Rates"}]]
    (fn []
      [:div.completed {:on-click #(swap! open? not)
                       :class    (when @open? "open")}
       [:a [:img {:src (:completed icons)}] "Completed data analysis" [:img {:src (:dropdown icons)}] [:div.notification (str total-unseen " New")]]
       [:input.search-input {:value       @search-text
                             :on-change   #(reset! search-text (-> % .-target .-value))
                             :type        "text"
                             :placeholder "Search..."}]
       [:ul.menu-items
        (doall
          (map (fn [{:keys [id readable-name of-type seen?] :as item}]
                 [:li.menu-item {:key id}
                  [completed-menu-item item]])
               data))]])))

;; TODO : gql search query : completed by readable-name
(defn queue [{:keys [open?]}]
  (let [search-text   (reagent/atom "")
        open?         (reagent/atom open?)
        total-ongoing 2
        data          []
        ]
    (fn []
      #_[:div.completed {:on-click #(swap! open? not)
                         :class    (when @open? "open")}
         #_[:a [:img {:src (:completed icons)}] "Completed data analysis" [:img {:src (:dropdown icons)}] [:div.notification (str total-unseen " New")]]
         #_[:input.search-input {:value       @search-text
                                 :on-change   #(reset! search-text (-> % .-target .-value))
                                 :type        "text"
                                 :placeholder "Search..."}]
         #_[:ul.menu-items
            (doall
              (map (fn [{:keys [id readable-name of-type seen?] :as item}]
                     [:li.menu-item {:key id}
                      [:div {:on-click #(re-frame/dispatch [:router/navigate :route/new-analysis nil {:tab                      (case of-type
                                                                                                                                  "Continuous: MCC Tree"    "continuous-mcc-tree"
                                                                                                                                  "Continuous: Time slices" "continuous-time-slices"
                                                                                                                                  "Discrete: MCC Tree"      "discrete-mcc-tree"
                                                                                                                                  "Discrete: Rates"         "discrete-rates"
                                                                                                                                  nil) :id id}])}
                       [:div readable-name (when-not seen? [:div "New"]) [:img {:src (:more icons)}]]
                       [:div of-type]]])
                   data))]])))

;; TODO : all menus
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
       [queue {:open? false}]]]]))

(defn app-container []
  (fn [child-page]
    [:div.grid-container
     [:div.app-header
      [header]]
     [main-menu]
     [:div.main
      child-page]]))
