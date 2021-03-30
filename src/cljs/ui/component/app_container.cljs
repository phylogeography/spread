(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [reagent.core :as reagent]
            [ui.component.icon :refer [icons icon-with-label]]
            [taoensso.timbre :as log]))

; TODO : open/close menu
(defn user-login []
  (let [open? (reagent/atom false)]
    (fn [{:keys [email]}]
      [:div.user-login
       [:button email]
       [:div.dropdown-content {:on-mouse-over #(swap! open? not)
                               :class (when @open? "open")}
        [:a {:on-click #(prn "TODO: logout")} "Log out"]
        [:a {:on-click #(prn "TODO: clear-data")} "Clear data"]
        [:a {:on-click #(prn "TODO: delete-account")} "Delete account"]]])))

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

;; TODO : all menus
;; https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/bfa17d6e-7b48-4547-8af8-b975b452dd35/
(defn main-menu []
  (fn []
    [:div.main-menu
     [:ul.main-menu-navigation
      [:li.nav-item
       [run-new {:open? true}]]



      ]]))

(defn app-container []
  (fn [child-page]
    [:div.grid-container
     [:div.app-header
      [header]]
     [main-menu]
     [:div.main
      child-page]]))
