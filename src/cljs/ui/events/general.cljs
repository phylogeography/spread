(ns ui.events.general
  (:require [re-frame.core :as re-frame]
            [ui.router.queries :as router-queries]
            [taoensso.timbre :as log]))

(def socket-id :default)

(defn active-page-changed [{:keys [db]}]
  (let [{:keys [name] :as active-page} (router-queries/active-page db)]
    (log/info "Active page changed" active-page)
    (case name
      :route/splash {:dispatch [:splash/initialize-page]}
      :route/home   {:dispatch [:home/initialize-page]}
      nil)))

(defn initialize [{:keys [db]} [_ config]]
  {:db             (assoc db :config config)
   :dispatch       [:websocket/connect socket-id {:url        (-> config :graphql :ws-url)
                                                   :format     :json
                                                   :on-connect [:graphql/ws-authorize
                                                                {:on-timeout [:graphql/ws-authorize-failed]}]
                                                   :protocols  ["graphql-ws"]}]
   :forward-events {:register    :active-page-changed
                    :events      #{:router/active-page-changed}
                    :dispatch-to [:active-page-changed]}})
