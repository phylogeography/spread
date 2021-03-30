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

;; TODO : left pane component
(defn app-container []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn [child-page]
      ;; (log/debug "app-layout/active-page" active-page)
      [:div.grid-container
       [:div.app-header
        [header]]
       [:div.app-left-pane

        [:ui
         [:li "@1"]
         [:li "@2"]
         [:li "@3"]
         [:li "@4"]
         [:li "@5"]
         [:li "@6"]
         [:li "@7"]
         ]

        ]
       [:div.main
        child-page]])))
