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

(defn screen-coord->proj-coord [translate scale [screen-x screen-y]]
  (let [[tx ty] translate]
    [(quot (- screen-x tx) scale)
     (quot (- screen-y ty) scale)]))

(def max-scale 2)
(def min-scale 0.1)

(defn zoom [{:keys [map-state] :as db} delta screen-coords]
  (let [{:keys [translate scale]} map-state
        zoom-dir (if (pos? delta) -1 1)
        [proj-x proj-y] (screen-coord->proj-coord translate scale screen-coords)]
    (update db :map-state
            (fn [{:keys [translate scale] :as map-state}]
              (let [new-scale (+ scale (* zoom-dir 0.8))
                    scaled-proj-x (* proj-x scale)
                    scaled-proj-y (* proj-y scale)
                    new-scaled-proj-x (* proj-x new-scale)
                    new-scaled-proj-y (* proj-y new-scale)
                    x-scale-diff (- scaled-proj-x new-scaled-proj-x)
                    y-scale-diff (- scaled-proj-y new-scaled-proj-y)
                    [tx ty] translate
                    new-translate-x (+ tx x-scale-diff)
                    new-translate-y (+ ty y-scale-diff)]
                (if true #_(< min-scale new-scale max-scale)
                  (assoc map-state
                         :translate [(int new-translate-x) (int new-translate-y)]
                         :scale new-scale)
                  map-state))))))

(re-frame/reg-event-db
 ::zoom
 (fn [{:keys [map-view-box-center] :as db} [_ {:keys [delta x y]}]]
   (zoom db delta [x y])))

(re-frame/reg-event-db
 ::map-grab (fn [{:keys [map-view-box-center] :as db} [_ {:keys [x y]}]]
              (-> db
                  (assoc-in [:map-state :grab :screen-origin]  [x y])
                  (assoc-in [:map-state :grab :screen-current]  [x y]))))

(re-frame/reg-event-db
 ::map-grab-release (fn [db _]
                      (update db :map-state dissoc :grab)))

(defn drag [{:keys [map-state] :as db} current-screen-coord]
  (if-let [{:keys [screen-origin]} (:grab map-state)]
    (let [{:keys [translate scale]} map-state
          [screen-x screen-y] current-screen-coord
          [current-proj-x current-proj-y] (screen-coord->proj-coord translate scale current-screen-coord)
          before-screen-coord (-> map-state :grab :screen-current)
          [drag-x drag-y] (let [[before-x before-y] (screen-coord->proj-coord translate scale before-screen-coord)]
                            [(- current-proj-x before-x) (- current-proj-y before-y)])

          db' (assoc-in db [:map-state :grab :screen-current] current-screen-coord)]
      (let [[before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db'
            (update-in [:map-state :translate 0] + screen-drag-x)
            (update-in [:map-state :translate 1] + screen-drag-y))))
    db))

(re-frame/reg-event-db
 ::map-drag (fn [{:keys [map-state] :as db} [_ {:keys [x y]}]]
              (drag db [x y])
              #_(let [view-box (math-utils/outscribing-rectangle map-view-box-center map-view-box-radius) 
                    [origin-x origin-y] (screen-coord->projection-coord view-box (:origin map-grab))
                    [drag-x drag-y]     (screen-coord->projection-coord view-box [x y])
                    [old-cx old-cy] (:center map-grab)
                    dragged-center [(+ old-cx (- origin-x drag-x)) (+ old-cy (- origin-y drag-y))]]
                (-> db
                    (assoc :map-view-box-center dragged-center)))))
