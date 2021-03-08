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

;; join both in map-size
(def map-width    700)
(def map-height   700)

(defn screen-coord->proj-coord [translate scale [screen-x screen-y]]
  (let [[tx ty] translate]
    [(quot (- screen-x tx) scale)
     (quot (- screen-y ty) scale)]))

#_(defn calc-zoom-for-view-box [x1 y1 x2 y2]
  (let [scale (/ map-width
                 (max (- x2 x1) (- y2 y1)))
        tx    (* -1 scale x1)
        ty    (* -1 scale y1)]
    
    (println "Fitting to a scale of " scale " and a translation of " [tx ty])
    {:translate [tx ty]
     :scale     scale}))

(defn calc-zoom-for-view-box [x1 y1 x2 y2]
  (let [scale (/ map-width
                 (max (- x2 x1) (- y2 y1)))
        tx    (* -1 scale x1)
        ty    (* -1 scale y1)]
    
    (println "Fitting to a scale of " scale " and a translation of " [tx ty])
    {:translate [tx ty]
     :scale     scale}))

(re-frame/reg-event-db
 ::map-set-view-box
 (fn [db [_ {:keys [x1 y1 x2 y2]}]]
   (let [{:keys [translate scale]} (:map-state db)]
     (println "Setting view box to " [x1 y1] [x2 y2])
     db
     (let [{:keys [translate scale]} (calc-zoom-for-view-box x1 y1 x2 y2)]
       (-> db
           (assoc :map-state {:translate translate
                              :scale     scale}))))))


(re-frame/reg-event-fx
 ::load-data
 (fn [{:keys [db]} [_ _]]
   (let [data-points [{:x1 104  :y1  110 :x2 120 :y2 130}
                      #_{:x1 108  :y1  120 :x2 110 :y2 140}
                      {:x1 108  :y1  120 :x2 120 :y2 121}
                      #_{:x1 180  :y1  90 :x2 185 :y2 95}]
         points (mapcat (fn [{:keys [x1 y1 x2 y2]}]
                          [[x1 y1] [x2 y2]])
                        data-points)
         x1 (apply min (map first points))
         y1 (apply min (map second points))
         x2 (apply max (map first points))
         y2 (apply max (map second points))]

     {:db (-> db
              (assoc :data-points data-points))
      ;; :dispatch [::map-set-view-box {:x1 x1 :y1 y1
      ;;                                :x2 x2 :y2 y2}]
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
 ::map-grab
 (fn [{:keys [map-view-box-center] :as db} [_ {:keys [x y]}]]
   (-> db
       (assoc-in [:map-state :grab :screen-origin]  [x y])
       (assoc-in [:map-state :grab :screen-current]  [x y]))))

(re-frame/reg-event-db
 ::map-grab-release
 (fn [db _]
   (update db :map-state dissoc :grab)))

(defn drag [{:keys [map-state] :as db} current-screen-coord]
  (if-let [{:keys [screen-origin]} (:grab map-state)]
    (let [{:keys [translate scale]} map-state
          [screen-x screen-y] current-screen-coord
          [current-proj-x current-proj-y] (screen-coord->proj-coord translate scale current-screen-coord)
          before-screen-coord (-> map-state :grab :screen-current)
          [drag-x drag-y] (let [[before-x before-y] (screen-coord->proj-coord translate scale before-screen-coord)]
                            [(- current-proj-x before-x) (- current-proj-y before-y)])]
      (let [[before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db            
            (assoc-in  [:map-state :grab :screen-current] current-screen-coord)
            (update-in [:map-state :translate 0] + screen-drag-x)
            (update-in [:map-state :translate 1] + screen-drag-y))))
    db))

(re-frame/reg-event-db
 ::map-drag
 (fn [{:keys [map-state] :as db} [_ {:keys [x y]}]]
   (drag db [x y])))

(re-frame/reg-event-db
 ::map-zoom-rectangle-grab
 (fn [{:keys [map-state] :as db} [_ {:keys [x y]}]]
   (-> db
       (assoc-in [:map-state :zoom-rectangle] {:origin [x y] :current [x y]}))))

(re-frame/reg-event-db
 ::map-zoom-rectangle-update
 (fn [{:keys [map-state] :as db} [_ {:keys [x y]}]]
   (when (:zoom-rectangle map-state)
     (assoc-in db [:map-state :zoom-rectangle :current] [x y]))))

(re-frame/reg-event-fx
 ::map-zoom-rectangle-release
 (fn [{:keys [db]} _]
   (let [{:keys [map-state]} db
         {:keys [translate scale zoom-rectangle]} map-state
         {:keys [origin current]} zoom-rectangle
         [x1 y1] (screen-coord->proj-coord translate scale origin)              
         [x2 y2] (screen-coord->proj-coord translate scale current)]     
     {:db (update db :map-state dissoc :zoom-rectangle)
      :dispatch [::map-set-view-box {:x1 x1 :y1 y1
                                     :x2 x2 :y2 y2}]})))
