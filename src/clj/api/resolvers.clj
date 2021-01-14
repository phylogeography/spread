(ns api.resolvers
  (:require [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
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

(defn get-discrete-tree
  [{:keys [db]} {id :id :as args} _]
  (log/info "get-discrete-tree" args)
  (clj->gql (discrete-tree-model/get-tree db {:id id})))

(defn discrete-tree->attributes
  [{:keys [db]} _ {tree-id :id :as parent}]
  (log/info "discrete-tree->attributes" parent)
  (let [attributes (map :attribute-name (discrete-tree-model/get-attributes db {:tree-id tree-id}))]
    (log/info "discrete-tree->attributes" {:attributes attributes})
    attributes))

(defn get-time-slicer
  [{:keys [db]} {id :id :as args} _]
  (log/info "get-time-slicer" args)
  (clj->gql (time-slicer-model/get-time-slicer db {:id id})))

(defn time-slicer->attributes
  [{:keys [db]} _ {time-slicer-id :id :as parent}]
  (log/info "time-slicer->attributes" parent)
  (let [attributes (map :attribute-name (time-slicer-model/get-attributes db {:time-slicer-id time-slicer-id}))]
    (log/info "time-slicer->attributes" {:attributes attributes})
    attributes))
