(ns spread.routing
  (:require [spread.api :as api]
            [compojure.core :as compojure :refer [ANY POST]]))

(defn wrap-context [handler context]
  (fn [request]
    (-> request
        (assoc :context context)
        handler)))

(compojure/defroutes routes
  (POST "/query" [] api/query)
  (ANY "*" [] (constantly {:status 404
                           :headers {"Content-Type" "application/json"}
                           :body "invalid route"})))

(defn create-handler [{:keys [context]}]
  (-> routes
      (wrap-context context)))
