(ns api.models.time-slicer
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-time-slicer)
(declare get-time-slicer)
(declare get-time-slicer-by-continuous-tree-id)
(declare insert-attribute)
(declare get-attributes)

(hugsql/def-db-fns "sql/time_slicer.sql")
(hugsql/def-sqlvec-fns "sql/time_slicer.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-time-slicer
  {:id                                      nil
   :continuous-tree-id                      nil
   :trees-file-url                          nil
   :slice-heights-file-url                  nil
   :burn-in                                 nil
   :number-of-intervals                     nil
   :relaxed-random-walk-rate-attribute-name nil
   :trait-attribute-name                    nil
   :hpd-level                               nil
   :contouring-grid-size                    nil
   :timescale-multiplier                    nil
   :most-recent-sampling-date               nil
   :output-file-url                         nil})

(defn upsert! [db {:keys [id] :as time-slicer}]
  (let [prev (or (get-time-slicer db {:id id})
                 nil-time-slicer)
        time-slicer (merge prev time-slicer)]
    (log/debug "upsert-time-slicer!" time-slicer)
    (upsert-time-slicer db time-slicer)))

(defn insert-attributes! [db id attributes]
  (doseq [att attributes]
    (insert-attribute db {:id id :attribute-name att})))
