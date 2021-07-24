(ns ui.config
  (:require [shared.macros :refer [get-env-variable]]))

(def environment (get-env-variable "SPREAD_ENV"))
(def google-client-id (get-env-variable "GOOGLE_CLIENT_ID" :required))
(def public-key (get-env-variable "PUBLIC_KEY" :required))
(def version "0.1.0")

(def default-config
  {:logging
   {:level    :info
    :console? true
    :sentry   false}

   :router
   {:routes        [["/" :route/splash]
                    ["/home" :route/home]
                    ["/new-analysis" :route/new-analysis]
                    ["/analysis-results" :route/analysis-results]
                    ["/documentation" :route/documentation]
                    ["/map" :route/map]]
    :default-route :route/splash
    :scroll-top?   true
    :html5?        true}

   :graphql
   {:ws-url "ws://127.0.0.1:3001/ws"
    :url    "http://127.0.0.1:3001/api"}

   :root-url "http://localhost:8020"

   :google
   {:client-id    google-client-id
    :redirect-uri "http://localhost:8020/?auth=google"}

   :public-key public-key})

(def dev-config
  (-> default-config
      (assoc-in [:logging :level] :debug)))

;; TODO : fill with production values
(def prod-config
  (-> default-config
      (assoc-in [:logging :level] :info)
      (assoc-in [:graphql :ws-url] "TODO")
      (assoc-in [:graphql :url] "TODO")
      (assoc :root-url "TODO")
      (assoc-in [:google :redirect-url] "TODO")
      ))

(defn load []
  (case environment
    "dev" dev-config
    "prod"  prod-config
    dev-config))
