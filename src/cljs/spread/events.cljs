(ns spread.events
  (:require [re-frame.core :as re-frame]
            [spread.events.animation :as events.animation]
            [spread.math-utils :as math-utils]
            [spread.db :as db]
            [ajax.core :as ajax]))

(re-frame/reg-event-fx
 ::initialize
 (fn [_ _]
   {:db db/empty-db
    :fx [[:dispatch [::load-map {:map/name "world/worldLow.json"}]]
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
 (fn [{:keys [db]} [_ geo-json-map]]
   #_(println "MAP LOADED")
   #_(println geo-json-map)
   {:db (update db :maps conj geo-json-map)}))

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
                 :on-success      [::map-loaded]
                 :on-failure      [::bad-http-result]}}))

#_(defn view-box-bounding-box

  "Calculates a screen bounding box (in screen coordinates) given a `map-box` (in long, lat)
  adding `padding` around."
  
  [map-box padding]

  (let [[x1 y1] (svg-renderer/map-coord->screen-coord [(:min-long map-box) (:min-lat map-box)])
        [x2 y2] (svg-renderer/map-coord->screen-coord [(:max-long map-box) (:max-lat map-box)])]
    [(- (min x1 x2) (/ padding 2))
     (- (min y1 y2) (/ padding 2))
     (+ (Math/abs (- x1 x2)) padding)
     (+ (Math/abs (- y1 y2)) padding)]))

(defn calculate-data-view-box [data-points]
  {:center [110 110]
   :radius 20})

(re-frame/reg-event-fx
 ::load-data
 (fn [{:keys [db]} [_ _]]
   (let [data-points [{:x1 104  :y1  110 :x2 120 :y2 130}]
         {:keys [center radius]} (calculate-data-view-box data-points)]
     {:db (-> db
              (assoc :data-points data-points
                     :map-view-box-center center
                     :map-view-box-radius radius))})
   #_{:http-xhrio {:method          :get
                 :uri             (str "http://localhost:1234/data/")
                 :timeout         8000 
                 :response-format (ajax/json-response-format {:keywords? true}) 
                 :on-success      [::data-loaded]
                 :on-failure      [::bad-http-result]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maps manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def map-width    600)
(def map-height   600)

(defn screen-coord->projection-coord [view-box [x y]]
  [(+ (:x view-box) (* (/ x map-width)  (:w view-box)))
   (+ (:y view-box) (* (/ y map-height) (:h view-box)))])

(re-frame/reg-event-db
 ::zoom
 (fn [{:keys [map-view-box-center] :as db} [_ {:keys [in? x y]}]]   
   (let [[center-x center-y] map-view-box-center
         zoom-k 4
         zoom-center-factor 0.5
         zoom-f (if in? - +)
         ;; vector from center to mouse pos
         center-displacement-x (* (- x center-x) zoom-center-factor)
         center-displacement-y (* (- y center-y) zoom-center-factor)
         ]
    (-> db
        (update :map-view-box-radius #(zoom-f % zoom-k))
        (update :map-view-box-center (fn [[cx cy]]
                                       (println "x y" [x y])
                                       (println "Old center" [center-x center-y])
                                       (println "Displacement x" center-displacement-x)
                                       (println "Displacement y" center-displacement-y)
                                       (println "New center" [(+ cx center-displacement-x)
                                                              (+ cy center-displacement-y)])
                                       [(+ cx center-displacement-x)
                                        (+ cy center-displacement-y)]))))))

(re-frame/reg-event-db
 ::map-grab (fn [{:keys [map-view-box-center] :as db} [_ {:keys [x y]}]]
              (-> db
                  (assoc-in [:map-grab :origin]  [x y]) ;; this is in screen coordinates
                  (assoc-in [:map-grab :center]  map-view-box-center) ;; this is in projection coordinates
                  )))

(re-frame/reg-event-db
 ::map-grab-release (fn [db _]
                      (dissoc db :map-grab)))

(re-frame/reg-event-db
 ::map-drag (fn [{:keys [map-grab map-view-box-center map-view-box-radius] :as db} [_ {:keys [x y]}]]
              (let [view-box (math-utils/outscribing-rectangle map-view-box-center map-view-box-radius) 
                    [origin-x origin-y] (screen-coord->projection-coord view-box (:origin map-grab))
                    [drag-x drag-y]     (screen-coord->projection-coord view-box [x y])
                    [old-cx old-cy] (:center map-grab)
                    dragged-center [(+ old-cx (- origin-x drag-x)) (+ old-cy (- origin-y drag-y))]]
                (-> db
                    (assoc :map-view-box-center dragged-center)))))
