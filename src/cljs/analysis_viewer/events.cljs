(ns analysis-viewer.events
  (:require [analysis-viewer.events.maps :as events.maps]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :map/initialize events.maps/initialize)
(reg-event-fx :map/map-loaded events.maps/map-loaded)
(reg-event-fx :map/data-loaded events.maps/data-loaded)
(reg-event-fx :map/load-map events.maps/load-map)
(reg-event-fx :map/load-data events.maps/load-data)
(reg-event-fx :map/download-current-as-svg events.maps/download-current-as-svg)

;; map graphics manipulation
(reg-event-db :map/set-view-box events.maps/set-view-box)
(reg-event-db :map/zoom events.maps/zoom)
(reg-event-db :map/grab events.maps/map-grab)
(reg-event-db :map/grab-release events.maps/map-release)
(reg-event-db :map/drag events.maps/drag)
(reg-event-db :map/zoom-rectangle-grab events.maps/zoom-rectangle-grab)
(reg-event-db :map/zoom-rectangle-update events.maps/zoom-rectangle-update)
(reg-event-fx :map/zoom-rectangle-release events.maps/zoom-rectangle-release)
(reg-event-fx :map/toggle-show-world events.maps/toggle-show-world)
