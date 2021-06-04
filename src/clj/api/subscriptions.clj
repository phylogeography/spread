(ns api.subscriptions
  (:require [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [api.models.parser :as parser-model]
            [clojure.core.async :as async :refer [>! go go-loop]]
            [shared.utils :refer [clj->gql]]
            [taoensso.timbre :as log]))

(defn- create-status-subscription [sub-name callback]
  (fn [{:keys [authed-user-id db access-token]} {:keys [id]} source-stream]
    (log/debug "client subscribed to"  {:sub/name  sub-name
                                        :user/id   authed-user-id
                                        :parser/id id
                                        :token     access-token})
    ;; create the subscription
    (let [subscription-closed? (async/promise-chan)]
      (go-loop []
        (let [sleep    (async/timeout 1000)
              [_ port] (async/alts! [subscription-closed? sleep])]
          (when-not (= port subscription-closed?)
            (when-let [{:keys [status progress readable-name of-type]} (callback db id)]
              (source-stream (clj->gql {:id       id
                                        :readable-name readable-name
                                        :of-type of-type
                                        :status   status
                                        :progress (or progress 0)}))
              (recur)))))
      ;; return a function to cleanup the subscription
      (fn []
        (go
          (log/debug "subscription closed" {:sub/name    sub-name
                                            :user/id     authed-user-id
                                            :analysis/id id})
          (>! subscription-closed? true))))))

(defn create-parser-status-sub []
  (create-status-subscription "parser" (fn [db id]
                                         (parser-model/get-status db {:parser-id id}))))

(defn create-continuous-tree-parser-status-sub []
  (create-status-subscription "continuous-tree" (fn [db id]
                                                  (continuous-tree-model/get-status db {:tree-id id}))))

(defn create-discrete-tree-parser-status-sub []
  (create-status-subscription "discrete-tree" (fn [db id]
                                                (discrete-tree-model/get-status db {:tree-id id}))))

(defn create-bayes-factor-parser-status-sub []
  (create-status-subscription "bayes-factor" (fn [db id]
                                               (bayes-factor-model/get-status db {:bayes-factor-analysis-id id}))))

(defn create-time-slicer-parser-status-sub []
  (create-status-subscription "time-slicer" (fn [db id]
                                              (time-slicer-model/get-status db {:time-slicer-id id}))))
