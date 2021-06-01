(ns analysis-viewer.db
  (:require [clojure.spec.alpha :as s]))

(s/def :cartesian/coord (s/tuple number? number?))

(s/def :map/scale number?)
(s/def :map/translate :cartesian/coord)
(s/def :map/screen-current :cartesian/coord)
(s/def :map/grab (s/keys :req-un [:map/screen-current]))
(s/def :map/width (s/nilable number?))
(s/def :map/height (s/nilable number?))
(s/def :map/state (s/keys :req-un [:map/scale
                                   :map/translate]
                          :opt-un [:map/grab
                                   :map/width
                                   :map/height]))

(s/def :map/url string?)
(s/def :map/z-index number?)
(s/def :map/geo-json any?)
(s/def :map/data (s/keys :req [:map/url :map/z-index :map/geo-json]))
(s/def :maps/data (s/coll-of :map/data))
(s/def :analysis.data/object any?) ;; TODO: I think this can be specified
(s/def :analysis.data.object/id string?)
(s/def :analysis.data/type #{:ContinuousTree :DiscreteTree :BayesFactor})
(s/def :analysis/data (s/map-of :analysis.data.object/id :analysis.data/object)) 

(s/def :ui.collapsible-tabs/tabs (s/map-of keyword? (s/map-of keyword? boolean?)))
(s/def :ui.switch-buttons/states (s/map-of keyword? boolean?))
(s/def :parameter/border-color string?)
(s/def :parameter/polygons-opacity (s/and number? #(<= 0 % 1)))
(s/def :parameter/polygons-color string?)
(s/def :parameter/transitions-opacity (s/and number? #(<= 0 % 1)))
(s/def :parameter/transitions-curvature (s/and number? #(<= 0 % 2)))
(s/def :parameter/transitions-width (s/and number? #(<= 0 % 0.3)))
(s/def :parameter/circles-raduis number?)
(s/def :parameter/cirlces-color string?)
(s/def :parameter/nodes-color string?)
(s/def :parameter/nodes-size number?)
(s/def :parameter/labels-color string?)
(s/def :parameter/labels-size number?)

(s/def :ui/parameters (s/keys :req-un [:parameter/map-borders-color
                                       :parameter/polygons-color
                                       :parameter/polygons-opacity
                                       :parameter/transitions-color
                                       :parameter/transitions-curvature
                                       :parameter/transitions-width
                                       :parameter/circles-radius
                                       :parameter/circles-color
                                       :parameter/nodes-color
                                       :parameter/nodes-size
                                       :parameter/labels-color
                                       :parameter/labels-size]))

(s/def :animation/percentage (s/and number? #(<= 0 % 1)))
(s/def :animation/state #{:stop :play})

(s/def :analysis/selected-object-id :analysis.data.object/id)
(s/def :analysis/possible-objects-ids (s/coll-of :analysis.data.object/id))
(s/def :map/popup-coord :cartesian/coord)

(s/def ::db (s/keys :req [:map/state                          
                          :animation/percentage
                          :animation/state
                          :ui.collapsible-tabs/tabs
                          :ui.switch-buttons/states
                          :ui/parameters]
                    :opt [:map/data
                          :analysis/data
                          :analysis.data/type
                          :analysis/selected-object-id
                          :analysis/possible-objects-ids
                          :map/popup-coord]))

(defn initial-db []
  {:map/state {:scale 1
               :translate [0 0]
               :width nil
               :height nil}
   :animation/percentage 0
   :animation/state :stop
   :ui.collapsible-tabs/tabs {:parameters {:layer-visibility true,
                                           :map-color true,
                                           :polygon-opacity true}}
   :ui.switch-buttons/states {:show-map? true
                              :map-borders? true
                              :map-labels? true
                              :transitions? true
                              :circles? true
                              :nodes? true
                              :labels? true}
   :ui/parameters {:map-borders-color "#079DAB"
                   :polygons-color "#1C58D0"
                   :polygons-opacity 0.3
                   :transitions-color "#266C08"
                   :transitions-curvature 1
                   :transitions-width 0.1
                   :circles-radius 1
                   :circles-color "#EEBE53"
                   :nodes-color "#B20707"
                   :nodes-size 0.1
                   :labels-color "#ECEFF8"
                   :labels-size 0.5}})
