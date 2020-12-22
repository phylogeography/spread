(ns api.resolvers
  (:require [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [shared.utils :refer [clj->gql]]
            [taoensso.timbre :as log]))

;; TODO : write middleware/interceptor that logs args and results

(defn get-continuous-tree
  [{:keys [db]} {id :id :as args} _]
  (log/info "get-continuous-tree" args)
  (clj->gql (continuous-tree-model/get-tree db {:id id})))

(defn continuous-tree->attributes
  [{:keys [db]} _ {tree-id :id :as parent}]
  (log/info "continuous-tree->attributes" parent)
  (let [attributes (map :attribute-name (continuous-tree-model/get-attributes db {:tree-id tree-id}))]
    (log/info "continuous-tree->attributes" {:attributes attributes})
    attributes))

(defn continuous-tree->hpd-levels
  [{:keys [db]} _ {tree-id :id :as parent}]
  (log/info "continuous-tree->hpd-levels" parent)
  (let [levels (map :level (continuous-tree-model/get-hpd-levels db {:tree-id tree-id}))]
    (log/info "continuous-tree->hpd-levels" {:levels levels})
    levels))

;; TODO
(defn get-discrete-tree
  [{:keys [db]} {id :id :as args} _]
  (log/info "get-discrete-tree" args)
  (clj->gql (discrete-tree-model/get-tree db {:id id})))

;; TODO
(defn discrete-tree->attributes
  [{:keys [db]} _ {tree-id :id :as parent}]
  (log/info "discrete-tree->attributes" parent)
  (let [attributes (map :attribute-name (discrete-tree-model/get-attributes db {:tree-id tree-id}))]
    (log/info "discrete-tree->attributes" {:attributes attributes})
    attributes))
