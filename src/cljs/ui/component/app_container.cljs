(ns ui.component.app-container
  (:require [re-frame.core :as re-frame]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.component.icon :refer [icons icon-with-label]]
            [taoensso.timbre :as log]))

;; class="navbar navbar-expand-md navbar-dark bg-dark fixed-top"

;; TODO : left pane

(defn header []
  (let [authed-user (re-frame/subscribe [::subs/authorized-user])]
    (fn []
      (let [{:keys [email]} @authed-user]
        [:div.header
         [icon-with-label {:icon (:spread icons) :label "SPREAD"}]
         [:div.user-login
          email
          ]]))))

(defn app-container []
  (let [active-page (re-frame/subscribe [::router.subs/active-page])]
    (fn [child-page]
      (log/debug "app-layout/active-page" active-page)
      [:div.grid-container
       [:div.app-header
        [header]]
       [:div.app-left-pane]
       [:div.main
        child-page]])))
