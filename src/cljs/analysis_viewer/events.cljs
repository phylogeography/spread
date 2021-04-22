(ns analysis-viewer.events
  (:require [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.db :as db]
            [re-frame.core :refer [reg-event-db reg-event-fx] :as re-frame]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

;; sc (spec check) interceptor using `after`
(def sc (re-frame/after (partial check-and-throw ::db/db)))

(reg-event-fx :map/initialize [sc] events.maps/initialize)
(reg-event-fx :map/map-loaded [sc] events.maps/map-loaded)
(reg-event-fx :map/data-loaded [sc] events.maps/data-loaded)
(reg-event-fx :map/load-map [sc] events.maps/load-map)
(reg-event-fx :map/load-data [sc] events.maps/load-data)
(reg-event-fx :map/download-current-as-svg [sc] events.maps/download-current-as-svg)

;; map graphics manipulation
(reg-event-db :map/set-view-box [sc] events.maps/set-view-box)
(reg-event-db :map/zoom events.maps/zoom)
(reg-event-db :map/grab [sc] events.maps/map-grab)
(reg-event-db :map/grab-release [sc] events.maps/map-release)
(reg-event-db :map/drag events.maps/drag)
(reg-event-db :map/zoom-rectangle-grab [sc] events.maps/zoom-rectangle-grab)
(reg-event-db :map/zoom-rectangle-update [sc] events.maps/zoom-rectangle-update)
(reg-event-fx :map/zoom-rectangle-release [sc] events.maps/zoom-rectangle-release)
(reg-event-fx :map/toggle-show-world [sc] events.maps/toggle-show-world)
