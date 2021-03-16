(ns ui.main
  (:require
   [mount.core :as mount]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [taoensso.timbre :as log]
   [ui.config :as config]
   [ui.home.events :as home-events]
   [ui.home.page]

   ;; [websocket-fx.core :as websocket]
   ;; [haslett.format :as formats]
   ;; [haslett.client :as ws]
   ;; [cljs.core.async :as async]

   [day8.re-frame.forward-events-fx]

   [ui.graphql :as graphql]
   [ui.websocket-fx :as websocket]
   [ui.logging :as logging]
   [ui.router.component :refer [router]]
   [ui.router.core :as router]
   [ui.router.events :as router-events]
   [ui.router.queries :as router-queries]
   [ui.splash.events :as splash-events]
   [ui.splash.page]
   [ui.storage]
   [ui.utils]))

(def functional-compiler (r/create-compiler {:function-components true}))
(def socket-id :default)

(re-frame/reg-event-fx
  :active-page-changed
  (fn [{:keys [db]}]
    (let [{:keys [name] :as active-page} (router-queries/active-page db)]
      (log/info "Active page changed" active-page)
      (case name
        :route/splash {:dispatch [::splash-events/initialize-page]}
        :route/home   {:dispatch [::home-events/initialize-page]}
        nil))))

(re-frame/reg-event-fx
  ::ws-authorize-failed
  (fn [{:keys [db ]} [_ why?]]
    (log/warn "Failed to authorize websocket conection" {:error why?})
    {:dispatch [::router-events/navigate :route/splash]}))

(re-frame/reg-event-fx
  :ui/initialize
  (fn [{:keys [db]} [_ config]]
    {:db             (-> db
                         (assoc :config config))
     :dispatch       [::websocket/connect socket-id {:url        "ws://127.0.0.1:3001/ws"
                                                     :format     :json
                                                     :on-connect [::graphql/ws-authorize
                                                                  {:on-timeout [::ws-authorize-failed]}]
                                                     :protocols  ["graphql-ws"]}]
     :forward-events {:register    :active-page-changed
                      :events      #{::router-events/active-page-changed}
                      :dispatch-to [:active-page-changed]}}))

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
    (re-frame/dispatch-sync [:ui/initialize config])
    (rdom/render [router]
                 (.getElementById js/document "app")
                 functional-compiler)))

(defn ^:export init []
  (start))

;; (defn test-it []
;;   (async/go
;;     (let [conn (async/<! (ws/connect "ws://127.0.0.1:3001/ws" {:format    formats/json
;;                                                                :protocols ["graphql-ws"]}))]
;;       (log/debug "connection status" conn))))

(comment

  @(re-frame/subscribe [::websocket/status :default])

  (re-frame/dispatch [::graphql/ws-authorize])

  @(re-frame/subscribe [::websocket/open-subscriptions :default])

  (re-frame/dispatch [::router-events/navigate :route/home])

  )
