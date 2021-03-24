(ns api.resolvers
  (:require [api.models.bayes-factor :as bayes-factor-model]
            [api.models.continuous-tree :as continuous-tree-model]
            [api.models.discrete-tree :as discrete-tree-model]
            [api.models.time-slicer :as time-slicer-model]
            [api.models.user :as user-model]
            [clojure.data.json :as json]
            [shared.utils :refer [clj->gql]]
            [taoensso.timbre :as log]
            [com.walmartlabs.lacinia.schema :as schema]))

(defn get-authorized-user
  [{:keys [authed-user-id db]} _ _]
  (log/info "get-authorized-user" {:authed-user-id authed-user-id})
  (clj->gql (user-model/get-user-by-id db {:id authed-user-id})))

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

(defn get-bayes-factor-analysis
  [{:keys [db]} {id :id :as args} _]
  (log/info "get-bayes-factor-analysis" args)
  (clj->gql (bayes-factor-model/get-bayes-factor-analysis db {:id id})))

(defn bayes-factor-analysis->bayes-factors
  [{:keys [db]} _ {bayes-factor-analysis-id :id :as parent}]
  (log/info "bayes-factor-analysis->bayes-factors" parent)
  (let [{:keys [bayes-factors]} (bayes-factor-model/get-bayes-factors db {:bayes-factor-analysis-id bayes-factor-analysis-id})
        bayes-factors           (json/read-str bayes-factors)]
    (log/info "bayes-factor-analysis->bayes-factors" {:bayes-factors bayes-factors})
    (clj->gql bayes-factors)))

;; TODO: https://lacinia.readthedocs.io/en/latest/resolve/type-tags.html?highlight=fragments
;; TODO : opaque cursor
(defn search-user-analysis
  [{:keys [db authed-user-id]} {first-n :first last-n :last before :before after :after :as args} _]

  (log/info "search-user-analysis" args)

  (let [
        results (user-model/search-user-analysis db {:user-id authed-user-id})

        edges (cond->> results
                after  (drop-while #(not= after (:id %)))
                after  (drop 1)
                before (take-while #(not= before (:id %))))

        edges' (cond->> edges
                 first-n (take first-n)
                 last-n  (take-last last-n)
                 true    (map #(hash-map :cursor (:id %) :node %)))

        ;; TODO : match
        page-info (cond-> {:has-next-page false
                           :has-previous-page false}
                    (and last-n (< last-n (count edges)))                                         (assoc :has-previous-page true)
                    (and after (not-empty (take-while #(not= after (:id %)) results)))            (assoc :has-previous-page true)
                    (and first-n (< first-n (count edges)))                                       (assoc :has-next-page true)
                    (and before (not-empty (drop 1 (drop-while #(not= before (:id %)) results)))) (assoc :has-next-page true)
                    true                                                                          (assoc :start-cursor (-> edges' first :cursor))
                    true                                                                          (assoc :end-cursor (-> edges' last :cursor)))

        ]

    (log/debug "@@@" {:edges     edges'
                      :page-info page-info})

    (clj->gql {:total-count (count results)
               :edges       edges'
               :page-info   page-info})))
