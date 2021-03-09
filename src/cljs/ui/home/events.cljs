(ns ui.home.events
  (:require [taoensso.timbre :as log]
            [re-frame.core :as re-frame]))

;; TODO : dispatch initial query (analysis etc)
(re-frame/reg-event-fx
  ::initialize-page
  (fn [{:keys [db]}]
    (log/debug "home/initialize-page" db)))
