(ns ui.home.page
  (:require [ui.router.component :refer [page]]
            [ui.subscriptions :as subs]
            [re-frame.core :as re-frame]))

;; TODO : just for graphql subs POC
(def id "60b08880-03e6-4a3f-a170-29f3c75cb43f")

(defmethod page :route/home []
  (let [authed-user (re-frame/subscribe [::subs/authorized-user])
        parser-status (re-frame/subscribe [::subs/discrete-tree-parser id])]
      (fn []
        (let [{:keys [email]} @authed-user
              {:keys [status]} @parser-status]
            [:div
             [:p "HOME"]
             [:pre (str "current-user:" email)]
             [:pre (str id " status:" status)]
             ]))))
