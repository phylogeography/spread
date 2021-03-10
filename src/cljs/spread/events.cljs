(ns spread.events
  (:require [re-frame.core :as re-frame]
            [spread.events.animation :as events.animation]
            [spread.events.map :as events.map]
            [spread.subs :as subs]
            [spread.math-utils :as math-utils]
            [spread.db :as db]
            [ajax.core :as ajax]))

(re-frame/reg-event-fx
 ::initialize
 (fn [_ _]
   {:db db/empty-db
    :fx [[:dispatch [::load-map {:map/name "world/worldLow.json" :map/z-index 0}]]
         [:dispatch [::load-map {:map/name "countries/uruguayLow.json"}]]
         [:dispatch [::load-map {:map/name "countries/argentinaLow.json"}]]
         [:dispatch [::load-data {}]]]}))

(re-frame/reg-event-fx
 ::bad-http-result
 (fn [_ ev]
   (js/console.error ev)
   {}))

(re-frame/reg-event-fx
 ::map-loaded
 (fn [{:keys [db]} [_ map-data geo-json-map]]
   (println "Map " (:map/name map-data) "loaded.")
   {:db (update db :maps conj (-> map-data
                                  (update :map/z-index #(or % 100))
                                  (assoc :map/geo-json geo-json-map)))}))

(re-frame/reg-event-fx
 ::data-loaded
 (fn [_ ev]
   #_(js/console.log "DATA LOADED")
   #_(js/console.log ev)
   {}))

(re-frame/reg-event-fx
 ::load-map
 (fn [_ [_ map-data]]
   {:http-xhrio {:method          :get
                 :uri             (str "http://localhost:8000/maps/" (:map/name map-data))
                 :timeout         8000 
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::map-loaded map-data]
                 :on-failure      [::bad-http-result]}}))

(def data-example
  ;; in lon,lat (map-coord) https://www.latlong.net/
  [;;     Uruguay                     Spain
   {:from [-56.045698 -33.724340] :to [-3.627909, 39.909736]}
   ;;     Salto                       Montevideo
   {:from [-57.777694 -31.240985] :to [-56.150632 -34.903953]}])

(re-frame/reg-event-fx
 ::load-data
 (fn [{:keys [db]} [_ _]]
   (let [data-points (->> data-example
                          (map (fn [{:keys [from to]}]
                                 (let [[x1 y1] (math-utils/map-coord->proj-coord from)
                                       [x2 y2] (math-utils/map-coord->proj-coord to)]
                                  {:x1 x1
                                   :y1 y1
                                   :x2 x2
                                   :y2 y2}))))
         points (mapcat (fn [{:keys [x1 y1 x2 y2]}]
                          [[x1 y1] [x2 y2]])
                        data-points)
         x1 (apply min (map first points))
         y1 (apply min (map second points))
         x2 (apply max (map first points))
         y2 (apply max (map second points))
         padding 2]

     {:db (-> db
              (assoc :data-points data-points))
      :dispatch [:map/set-view-box {:x1 (- x1 padding) :y1 (- y1 padding)
                                    :x2 (+ x2 padding) :y2 (+ y2 padding)}]
      })
   #_{:http-xhrio {:method          :get
                 :uri             (str "http://localhost:1234/data/")
                 :timeout         8000 
                 :response-format (ajax/json-response-format {:keywords? true}) 
                 :on-success      [::data-loaded]
                 :on-failure      [::bad-http-result]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maps manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db :map/set-view-box events.map/set-view-box)

(re-frame/reg-event-db
 :map/zoom 
 (fn [{:keys [map-view-box-center] :as db} [_ {:keys [delta x y]}]]
   (events.map/zoom db delta [x y])))

(re-frame/reg-event-db :map/grab events.map/map-grab)

(re-frame/reg-event-db :map/grab-release events.map/map-release)

(re-frame/reg-event-db
 :map/drag
 (fn [{:keys [map-state] :as db} [_ {:keys [x y]}]]
   (events.map/drag db [x y])))

(re-frame/reg-event-db :map/zoom-rectangle-grab events.map/zoom-rectangle-grab)

(re-frame/reg-event-db :map/zoom-rectangle-update events.map/zoom-rectangle-update)

(re-frame/reg-event-fx :map/zoom-rectangle-release events.map/zoom-rectangle-release)

(re-frame/reg-event-fx
 :map/download-current-as-svg
 (fn [{:keys [db]} _]
   {:spread/download-current-map-as-svg {:geo-json-map (subs/geo-json-data-map (:maps db))
                                         :data-points (:data-points db)}}))
