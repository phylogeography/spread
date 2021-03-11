(ns ui.home.events
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

;; TODO : dispatch initial query (user analysis etc)
(re-frame/reg-event-fx
  ::initialize-page
  (fn [{:keys [db]}]
    (log/debug "home/initialize-page" db)))
