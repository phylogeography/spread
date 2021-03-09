(ns spread.events.map
  (:require [spread.math-utils :as math-utils]))

;; screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
;; proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
;; map-coord:    [lat,lon]  coordinates in map lat,long coords, -180 <= lon <= 180, -90 <= lat <= 90

(def map-screen-width    1200)
(def map-screen-height   600)

(def map-proj-width  360)
(def map-proj-height 180)

(def proj-scale (/ map-screen-width map-proj-width))

(def max-scale 22)
(def min-scale 1)

(defn set-view-box [db [_ {:keys [x1 y1 x2 y2]}]]
  (let [{:keys [translate scale]} (:map-state db)]
    (let [{:keys [translate scale]} (math-utils/calc-zoom-for-view-box x1 y1 x2 y2 proj-scale)]
      (-> db
          (assoc :map-state {:translate translate
                             :scale     scale})))))

(defn zoom [{:keys [map-state] :as db} delta screen-coords]
  (let [{:keys [translate scale]} map-state
        zoom-dir (if (pos? delta) -1 1)
        [proj-x proj-y] (math-utils/screen-coord->proj-coord translate scale proj-scale screen-coords)]
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

(defn map-grab [{:keys [map-view-box-center] :as db} [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map-state :grab :screen-origin]  [x y])
      (assoc-in [:map-state :grab :screen-current]  [x y])))

(defn map-release [db _]
  (update db :map-state dissoc :grab))

(defn drag [{:keys [map-state] :as db} current-screen-coord]
  (if-let [{:keys [screen-origin]} (:grab map-state)]
    (let [{:keys [translate scale]} map-state
          [screen-x screen-y] current-screen-coord
          [current-proj-x current-proj-y] (math-utils/screen-coord->proj-coord translate
                                                                               scale
                                                                               proj-scale
                                                                               current-screen-coord)
          before-screen-coord (-> map-state :grab :screen-current)
          [drag-x drag-y] (let [[before-x before-y] (math-utils/screen-coord->proj-coord translate
                                                                                         scale
                                                                                         proj-scale
                                                                                         before-screen-coord)]
                            [(- current-proj-x before-x) (- current-proj-y before-y)])]
      (let [[before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db            
            (assoc-in  [:map-state :grab :screen-current] current-screen-coord)
            (update-in [:map-state :translate 0] + screen-drag-x)
            (update-in [:map-state :translate 1] + screen-drag-y))))
    db))

(defn zoom-rectangle-grab [{:keys [map-state] :as db} [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map-state :zoom-rectangle] {:origin [x y] :current [x y]})))

(defn zoom-rectangle-update[{:keys [map-state] :as db} [_ {:keys [x y]}]]
  (when (:zoom-rectangle map-state)
    (assoc-in db [:map-state :zoom-rectangle :current] [x y])))

(defn zoom-rectangle-release [{:keys [db]} _]
  (let [{:keys [map-state]} db
        {:keys [translate scale zoom-rectangle]} map-state
        {:keys [origin current]} zoom-rectangle
        [x1 y1] (math-utils/screen-coord->proj-coord translate scale proj-scale origin)              
        [x2 y2] (math-utils/screen-coord->proj-coord translate scale proj-scale current)]     
    {:db (update db :map-state dissoc :zoom-rectangle)
     :dispatch [:map/set-view-box {:x1 x1 :y1 y1
                                   :x2 x2 :y2 y2}]}))


