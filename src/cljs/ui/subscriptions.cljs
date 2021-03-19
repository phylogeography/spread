(ns ui.subscriptions
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::config
  (fn [db _]
    (get db :config)))

(re-frame/reg-sub
  ::users
  (fn [db _]
    (-> db :users
        (dissoc :authorized-user))))

(re-frame/reg-sub
  ::authorized-user
  (fn [db _]
    (-> db :users :authorized-user)))

(re-frame/reg-sub
  ::discrete-tree-parsers
  (fn [db _]
    (get db :discrete-tree-parsers)))

(re-frame/reg-sub
  ::discrete-tree-parser
  :<- [::discrete-tree-parsers]
  (fn [discrete-tree-parsers [_ id]]
    (get discrete-tree-parsers id)))

(comment
  @(re-frame/subscribe [::authorized-user])
  @(re-frame/subscribe [::discrete-tree-parsers])
  @(re-frame/subscribe [::discrete-tree-parser "60b08880-03e6-4a3f-a170-29f3c75cb43f"]))