(ns api.resolvers
  (:require [api.db :as db]
            [shared.utils :refer [clj->gql]]
            [api.models.continuous-tree :as continuous-tree-model]
            [taoensso.timbre :as log]))

;; TODO : write middleware/interceptor that logs args and results

(defn continuous-tree->attributes
  [{:keys [db]} _ {tree-id :treeId :as parent}]
  (log/info "continuous-tree->attributes" parent)
  (let [result (map :attribute-name (continuous-tree-model/get-attributes db {:tree-id tree-id}))]
    (log/info "continuous-tree->attributes result" result)
    result))

(defn continuous-tree->hpd-levels
  [{:keys [db]} _ {tree-id :treeId :as parent}]
  (log/info "continuous-tree->hpd-levels" parent)
  (let [result (map :level (continuous-tree-model/get-hpd-levels db {:tree-id tree-id}))]
    (log/info "continuous-tree->hpd-levels result" result)
    result))

(defn get-continuous-tree
  [{:keys [db] :as ctx} {tree-id :treeId :as args} _]
  (log/info "get-continuous-tree" args)
  (clj->gql (continuous-tree-model/get-tree db {:tree-id tree-id})))

;; TODO : read status (from RDS)
(defn get-parser-status
  [{:keys [db]} _ args]
  (log/info "get-parser-execution" {:args args})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :SUCCEEDED
   :output "s3://spread-dev-uploads/4d07edcf-4b4b-4190-8cea-38daece8d4aa"})
