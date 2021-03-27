(ns ui.home.page
  (:require [re-frame.core :as re-frame]
            [ui.router.component :refer [page]]
            [ui.subscriptions :as subs]))

;; TODO : just for graphql subs POC
(def analysis-id "db6969bc-bf87-4ebe-919b-ff377bfe5992")

(defmethod page :route/home []
  (let [authed-user (re-frame/subscribe [::subs/authorized-user])
        parser-status (re-frame/subscribe [::subs/discrete-tree-parser analysis-id])]
      (fn []
        (let [{:keys [email]} @authed-user
              {:keys [status]} @parser-status]
            [:div
             [:p "HOME"]
             [:pre (str "current-user:" email)]
             [:pre (str analysis-id " status:" status)]
             ]))))
