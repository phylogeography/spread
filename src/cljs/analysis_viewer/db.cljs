(ns analysis-viewer.db
  (:require [clojure.spec.alpha :as s]))

(s/def :cartesian/coord (s/tuple number? number?))

(s/def :map/scale number?)
(s/def :map/translate :cartesian/coord)
(s/def :map/screen-origin :cartesian/coord)
(s/def :map/screen-current :cartesian/coord)
(s/def :map/grab (s/keys :req-un [:map/screen-origin :map/screen-current]))
(s/def :map/state (s/keys :req-un [:map/scale
                                   :map/translate]
                          :opt-un [:map/grab]))

(s/def :map/url string?)
(s/def :map/z-index number?)
(s/def :map/geo-json any?)
(s/def :map/data (s/keys :req [:map/url :map/z-index :map/geo-json]))
(s/def :maps/data (s/coll-of :map/data))
(s/def :analysis/data any?) ;; TODO: I think this can be specified

(s/def :ui.collapsible-tabs/tabs (s/map-of keyword? (s/map-of keyword? boolean?)))
(s/def :ui.switch-buttons/states (s/map-of keyword? boolean?))
(s/def :ui/parameters map?)

(s/def ::db (s/keys :req [:map/state
                          :ui.collapsible-tabs/tabs
                          :ui.switch-buttons/states
                          :ui/parameters]
                    :opt [:map/data
                          :analysis/data]))

(defn initial-db []
  {:map/state {:scale 1
               :translate [0 0]}
   :ui.collapsible-tabs/tabs {:parameters {:layer-visibility true,
                                           :map-color true,
                                           :polygon-opacity true}}
   :ui.switch-buttons/states {:map-borders true}
   :ui/parameters {:map-borders-color "#079DAB"}})
