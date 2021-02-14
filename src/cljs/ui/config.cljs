(ns ui.config
  (:require [shared.macros :refer [get-env-variable]]))

(def environment (get-env-variable "SPREAD_ENV"))
(def sentry-dsn (get-env-variable "SENTRY_DSN"))
(def version "0.1.0")

(def default-config
  {:logging
   {:level    :info
    :console? true
    :sentry false}

   :router
   {:routes        [["/" :route/home]]
    :default-route :route/home
    :scroll-top?   true
    :html5?        true}

   :graphql
   {:url "http://127.0.0.1:3001/api"}

   })

(def dev-config
  (-> default-config
      (assoc-in [:logging :level] :debug)))

(def qa-config
  (-> default-config
      (assoc-in [:logging :level] :info)
      (assoc-in [:logging :sentry] {:dsn         sentry-dsn
                                    :min-level   :warn
                                    :environment "QA"
                                    :release version})))

(defn load []
  (case environment
   "dev" dev-config
   "qa" qa-config
   dev-config))
