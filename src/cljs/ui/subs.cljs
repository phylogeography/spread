(ns ui.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::config
  (fn [db _]
    (get db :config)))
