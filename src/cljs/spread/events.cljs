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

(def map-screen-width    1200)
(def map-screen-height   600)

(def map-proj-width  360)
(def map-proj-height 180)

(def proj-scale (/ map-screen-width map-proj-width))

(def max-scale 22)
(def min-scale 1)

;; screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
;; proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
;; map-coord:    [lat,lon]  coordinates in map lat,long coords, -180 <= lon <= 180, -90 <= lat <= 90

(defn screen-coord->proj-coord [translate scale [screen-x screen-y]]
  (let [[tx ty] translate]
    ;; translate the screen-coord and scale it twice (one for the proj-scale and the other for the zoom scale)
    [(/ (- screen-x tx) (* proj-scale scale))
     (/ (- screen-y ty) (* proj-scale scale))]))

(defn calc-zoom-for-view-box [x1 y1 x2 y2]
  (let [scale-x (/ map-proj-width  (max (- x2 x1)))
        scale-y (/ map-proj-height (max (- y2 y1)))
        scale (min scale-x scale-y)
        tx    (* -1 scale proj-scale x1)
        ty    (* -1 scale proj-scale y1)]
    
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
                      
                      #_{:x1 108  :y1  120 :x2 120 :y2 121}
                      {:x1 99  :y1  120 :x2 125 :y2 121}
                      ]
         points (mapcat (fn [{:keys [x1 y1 x2 y2]}]
                          [[x1 y1] [x2 y2]])
                        data-points)
         x1 (apply min (map first points))
         y1 (apply min (map second points))
         x2 (apply max (map first points))
         y2 (apply max (map second points))]

     {:db (-> db
              (assoc :data-points data-points))
      :dispatch [::map-set-view-box {:x1 x1 :y1 y1
                                     :x2 x2 :y2 y2}]
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

(defn zoom [{:keys [map-state] :as db} delta screen-coords]
  (let [{:keys [translate scale]} map-state
        zoom-dir (if (pos? delta) -1 1)
        [proj-x proj-y] (screen-coord->proj-coord translate scale screen-coords)]
    (update db :map-state
            (fn [{:keys [translate scale] :as map-state}]
              (let [new-scale (+ scale (* zoom-dir 0.8))
                    scaled-proj-x (* proj-x scale proj-scale)
                    scaled-proj-y (* proj-y scale proj-scale)
                    new-scaled-proj-x (* proj-x new-scale proj-scale)
                    new-scaled-proj-y (* proj-y new-scale proj-scale)
                    x-scale-diff (- scaled-proj-x new-scaled-proj-x)
                    y-scale-diff (- scaled-proj-y new-scaled-proj-y)
                    [tx ty] translate
                    new-translate-x (+ tx x-scale-diff)
                    new-translate-y (+ ty y-scale-diff)]
                
                (if (< min-scale new-scale max-scale)
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

(re-frame/reg-fx
 ::download-current-map-as-svg
 (fn [_]   
   (let [svg-elem (js/document.getElementById "map-and-data")
         svg-text (.-outerHTML svg-elem)
         download-anchor (js/document.createElement "a")]

     (.setAttribute download-anchor "href" (str "data:image/svg+xml;charset=utf-8," (js/encodeURIComponent svg-text)))
     (.setAttribute download-anchor "download" "map.svg")
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor)
     (println "Done"))))

(re-frame/reg-event-fx
 ::download-current-map-as-svg
 (fn [_ _]
   {::download-current-map-as-svg nil}))
