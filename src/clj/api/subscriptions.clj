(ns api.subscriptions
  (:require [shared.utils :refer [clj->gql]]
            [taoensso.timbre :as log]
            [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]))

(defn continuous-tree-parser-status
  [{:keys [authed-user-id db] :as context} {:keys [id] :as args} source-stream]
  (log/debug "client subscribed to continuous-tree-parser-status" {:id      id
                                                                   :user/id authed-user-id})
  ;; create the subscription
  (loop []
    (try
      (when-let [{:keys [status]} (continuous-tree-model/get-status db {:id id})]
        (source-stream (clj->gql {:id     id
                                  :status status})))
      (Thread/sleep 1000)
      (catch Exception e
        (log/error "Subscription error" {:error e})))
    (recur))
  ;; return a function to cleanup the subscription
  (fn []
    (log/debug "continuous-tree-parser-status subscription closed" {:user/id authed-user-id
                                                                    :id      id})))
