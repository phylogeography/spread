(ns api.models.timeslicer
  (:require [hugsql.core :as hugsql]
            [taoensso.timbre :as log]))

;; These are just not to upset clj-kondo
(declare upsert-timeslicer)
(declare update-timeslicer)
(declare insert-attribute)

(hugsql/def-db-fns "sql/timeslicer.sql")
(hugsql/def-sqlvec-fns "sql/timeslicer.sql")

;; TODO: remove this when we figure out https://github.com/layerware/hugsql/issues/116
(def ^:private nil-timeslicer
  {:id nil
   :user-id nil
   :trees-file-url nil
   :slice-heights-file-url nil
   :status nil
   :readable-name nil
   :burn-in nil
   :relaxed-random-walk-rate-attribute-name nil
   :trait-attribute-name nil
   :hpd-level nil
   :contouring-grid-size nil
   :timescale-multiplier nil
   :most-recent-sampling-date nil
   :output-file-url nil
   :trees-count nil
   })

(defn upsert-timeslicer! [db timeslicer]
  (let [timeslicer (->> timeslicer
                  (merge nil-timeslicer)
                  (#(update % :status name)))]
    (log/debug "upsert-timeslicer!" timeslicer)
    (upsert-timeslicer db timeslicer)))

(defn update-timeslicer! [db timeslicer]
  (let [timeslicer (->> timeslicer
                  (merge nil-timeslicer)
                  (#(update % :status name)))]
    (log/debug "update-timeslicer!" timeslicer)
    (update-timeslicer db timeslicer)))

(defn insert-attributes! [db timeslicer-id attributes]
  (doseq [att attributes]
    (insert-attribute db {:timeslicer-id timeslicer-id :attribute-name att})))
