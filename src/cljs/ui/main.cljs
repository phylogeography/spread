(ns ui.main
  (:require
   [day8.re-frame.forward-events-fx]
   [mount.core :as mount]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [taoensso.timbre :as log]
   [ui.config :as config]
   [ui.events]
   [ui.home.page]
   [ui.logging :as logging]
   [ui.router.component :refer [router]]
   [ui.router.core :as router]
   [ui.splash.page]
   [ui.storage]
   [ui.utils]
   [ui.websocket-fx :as websocket]
   ))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn ^:dev/before-load stop []
  (log/debug "Stopping...")
  (mount/stop))

(defn ^:dev/after-load start []
  (let [config (config/load)]
    (js/console.log "Starting..." )
    (-> (mount/only #{#'logging/logging
                      #'router/router})
        (mount/with-args config)
        (mount/start)
        (as-> $ (log/info "Started" {:components $
                                     :config     config})))
    (re-frame/dispatch-sync [:general/initialize config])
    (rdom/render [router]
                 (.getElementById js/document "app")
                 functional-compiler)))

(defn ^:export init []
  (start))

(comment
  @(re-frame/subscribe [::websocket/status :default])
  (re-frame/dispatch [:graphql/ws-authorize])
  @(re-frame/subscribe [::websocket/open-subscriptions :default])
  (re-frame/dispatch [:router/navigate :route/home]))
