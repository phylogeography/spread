(ns spread.api
  (:require [ring.util.response :as response]
            [taoensso.timbre :as log]
            [spread.messaging :as messaging]))

(def +http-ok+ 200)
(def +http-internal-server-error+ 500)
(def +http-bad-request+ 400)

(def +internal-server-error+ "Internal Server Error")

(defn make-response
  [http-status body]
  (-> (response/response body)
      (response/status http-status)))

(defn query [{:keys [context body] :as request}]
  (try
    (with-open [channel (messaging/open-channel)]

      (log/info "received request" request)

      ;; publish message for workers to
      (messaging/publish channel
                         {:queue (:queue-name context)}
                         :spread/query
                         {:sleep (rand-int 10000)})

      {:status 200
       :body "OK"})
    (catch Throwable e
      (log/error "query error" {:error e})
      (make-response +http-internal-server-error+ +internal-server-error+))))
