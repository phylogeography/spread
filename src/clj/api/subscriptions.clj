(ns api.subscriptions
  (:require [api.models.continuous-tree :as continuous-tree-model]
            [clojure.core.async :refer [<! close! go-loop timeout]]
            [shared.utils :refer [clj->gql]]
            [taoensso.timbre :as log]))

(defn continuous-tree-parser-status
  [{:keys [authed-user-id db] :as context} {:keys [id] :as args} source-stream]
  (log/debug "client subscribed to continuous-tree-parser-status" {:id      id
                                                                   :user/id authed-user-id})
  ;; create the subscription
  (let [subscription (go-loop []
                       (when-let [{:keys [status]} (continuous-tree-model/get-status db {:id id})]
                         (source-stream (clj->gql {:id     id
                                                   :status status}))
                         (<! (timeout 1000))
                         (recur)))]
    ;; return a function to cleanup the subscription
    (fn []
      (log/debug "continuous-tree-parser-status subscription closed" {:user/id authed-user-id
                                                                      :id id})
      (close! subscription))))
