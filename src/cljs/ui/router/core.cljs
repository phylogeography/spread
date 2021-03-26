(ns ui.router.core
  (:require [bide.core :as bide]
            [clojure.string :as string]
            [mount.core :as mount :refer [defstate]]
            [re-frame.core :refer [dispatch dispatch-sync]]))

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
                         :on-navigate #(dispatch [:router/active-page-change %1 %2 %3])})
    (dispatch-sync [:router/start opts])
    opts))

(defn stop []
  (dispatch-sync [:router/stop]))

(defstate router
  :start (start (:router (mount/args)))
  :stop (stop))
