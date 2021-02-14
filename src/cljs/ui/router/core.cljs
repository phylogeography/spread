(ns ui.router.core
  (:require [bide.core :as bide]
            [clojure.string :as string]
            [mount.core :as mount :refer [defstate]]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [ui.router.events :as events]))

(declare router)

(defn- hostname-html5-host? [html5-hosts]
  (when html5-hosts
    (contains? (cond-> html5-hosts
                 (string? html5-hosts) (string/split ",")
                 true                  set)
               (aget js/window "location" "hostname"))))

(defn start [{:keys [:routes :html5? :default-route :html5-hosts] :as opts}]
  (let [router (bide/router routes)
        html5? (or html5? (hostname-html5-host? html5-hosts))
        opts   (merge opts
                      {:bide-router router
                       :html5?      html5?})]
    (bide/start! router {:html5?      html5?
                         :default     default-route
                         :on-navigate #(dispatch [::events/active-page-changed* %1 %2 %3])})
    (dispatch-sync [::events/start opts])
    opts))

(defn stop []
  (dispatch-sync [::events/stop]))

(defstate router
  :start (start (:router (mount/args)))
  :stop (stop))
