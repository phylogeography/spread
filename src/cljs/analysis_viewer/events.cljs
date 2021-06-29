(ns analysis-viewer.events
  (:require [analysis-viewer.db :as db]
            [analysis-viewer.events.filters :as events.filters]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.events.ui :as events.ui]            
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [re-frame.core :refer [reg-event-db reg-event-fx] :as re-frame]))

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
(reg-event-db :map/zoom-inc events.maps/zoom-inc)
(reg-event-db :map/zoom events.maps/zoom)
(reg-event-db :map/grab [sc] events.maps/map-grab)
(reg-event-db :map/grab-release [sc] events.maps/map-release)
(reg-event-db :map/drag events.maps/drag)
(reg-event-db :map/zoom-rectangle-grab [sc] events.maps/zoom-rectangle-grab)
(reg-event-db :map/zoom-rectangle-update [sc] events.maps/zoom-rectangle-update)
(reg-event-fx :map/zoom-rectangle-release [sc] events.maps/zoom-rectangle-release)
(reg-event-fx :map/toggle-show-world [sc] events.maps/toggle-show-world)

(reg-event-db :map/show-object-attributes [sc] events.maps/show-object-attributes)
(reg-event-db :map/hide-object-attributes [sc] events.maps/hide-object-attributes)
(reg-event-db :map/show-object-selector [sc] events.maps/show-object-selector)
(reg-event-db :map/hide-object-selector [sc] events.maps/hide-object-selector)
(reg-event-db :map/highlight-object [sc] events.maps/highlight-object)

(reg-event-db :collapsible-tabs/toggle [sc] events.ui/toggle-collapsible-tab)
(reg-event-db :switch-button/toggle [sc] events.ui/toggle-switch-button)
(reg-event-db :parameters/select [sc] events.ui/parameters-select)

(reg-event-db :animation/prev [sc] events.maps/animation-prev)
(reg-event-db :animation/next [sc] events.maps/animation-next)
(reg-event-fx :animation/toggle-play-stop [sc] events.maps/animation-toggle-play-stop)
(reg-event-db :animation/set-crop [sc] events.maps/animation-set-crop)
(reg-event-db :animation/set-speed [sc] events.maps/animation-set-speed)

(reg-event-db :filters/add-attribute-filter [sc] events.filters/add-attribute-filter)
(reg-event-db :filters/rm-attribute-filter [sc] events.filters/rm-attribute-filter)
(reg-event-db :filters/set-linear-attribute-filter-range [sc] events.filters/set-linear-attribute-filter-range)
(reg-event-db :filters/add-ordinal-attribute-filter-item [sc] events.filters/add-ordinal-attribute-filter-item)
(reg-event-db :filters/rm-ordinal-attribute-filter-item [sc] events.filters/rm-ordinal-attribute-filter-item)

(reg-event-fx :ticker/tick [] events.maps/ticker-tick)

(reg-event-db :map/set-dimensions [sc] events.maps/set-dimensions)
