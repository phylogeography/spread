(ns ui.splash.events
  (:require [taoensso.timbre :as log]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  ::initialize-page
  (fn []
    (log/debug "splash/initialize-page")))
