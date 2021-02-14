(ns ui.main
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [ui.config :as config]
            [ui.router.core :as router]
            [ui.router.component :refer [router]]
            [ui.router.events :as router-events]
            [mount.core :as mount]
            ui.home.page
            day8.re-frame.forward-events-fx
            [ui.logging :as logging]
            [taoensso.timbre :as log]
            [re-frame.core :as re-frame]
            [ui.home.events :as home-events]
            ))

(def functional-compiler (r/create-compiler {:function-components true}))

(re-frame/reg-event-fx
  :active-page-changed
  (fn [{:keys [:db]}]
    (let [{:keys [name] :as active-page} (:active-page db)]
      (log/info "Active page changed" active-page)
      (case name
        :route/home {:dispatch [:home-events/initialize-page]}
        nil))))

(re-frame/reg-event-fx
  :ui/initialize
  (fn [{:keys [db]} [_ config]]
    {:db (assoc db :spread/config config)
     :forward-events {:register    :active-page-changed
                      :events      #{:router-events/active-page-changed}
                      :dispatch-to [:active-page-changed]}}))

(defn ^:dev/before-load stop []
  (log/debug "Stopping...")
  #_(mount/stop))

(defn ^:dev/after-load start []
  (let [config (config/load)]
    (js/console.log "Starting..." )

    (-> (mount/only #{#'logging/logging
                      #'router/router})
        (mount/with-args config)
        (mount/start)
        (as-> $ (log/info "Started" {:components $
                                     :config     config})))

    (re-frame/dispatch-sync [:ui/initialize config])

    (rdom/render [router]
                 (.getElementById js/document "app")
                 functional-compiler)))

(defn ^:export init []
  (start))
