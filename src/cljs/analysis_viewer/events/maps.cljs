(ns analysis-viewer.events.maps
  "Handle events registered in events.cljs under :map/*"
  (:require [ajax.core :as ajax]
            [analysis-viewer.db :as db]
            [analysis-viewer.map-emitter :as map-emitter]
            [analysis-viewer.subs :as subs]
            [clojure.string :as string]
            [shared.math-utils :as math-utils]))

;; screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
;; proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
;; map-coord:    [lat,lon]  coordinates in map lat,long coords, -180 <= lon <= 180, -90 <= lat <= 90

(def map-proj-width  360)
(def map-proj-height 180)

(def max-scale 22)
(def min-scale 0.8)

;; TODO: grab this from config
(def s3-bucket-url "http://127.0.0.1:9000/spread-dev-uploads/")

(defn initialize [_ [_ maps analysis-type analysis-data-url]]
  (let [load-maps-events (->> maps
                              (mapv (fn [map-code]
                                      (let [world-map? (string/ends-with? map-code "WORLD")]
                                        [:map/load-map (cond-> {:map/url (str s3-bucket-url "maps/country-code-maps/" map-code ".json")}
                                                         world-map? (assoc :map/z-index 0))]))))
        load-data-event [:map/load-data analysis-type analysis-data-url]]
    {:db (db/initial-db) 
     :fx (->> (conj load-maps-events load-data-event)
              (map (fn [ev] [:dispatch ev])))}))

(defn map-loaded [{:keys [db]} [_ map-data geo-json-map]]
  {:db (update db :maps/data conj (-> map-data
                                      (update :map/z-index #(or % 100))
                                      (assoc :map/geo-json geo-json-map)))})

(defn get-analysis-objects-view-box [objects]
  (let [all-coords (->> objects
                        (mapcat (fn [o]
                                  (case (:type o)
                                    :arc [(:from-coord o) (:to-coord o)]
                                    :point [(:coord o)]
                                    :area (:coord o)))))]
    {:x1 (apply min (map first all-coords))
     :y1 (apply min (map second all-coords))
     :x2 (apply max (map first all-coords))
     :y2 (apply max (map second all-coords))}))

(defn data-loaded [{:keys [db]} [_ analysis-type data]]
  (let [analysis-data (case analysis-type
                        :continuous-tree (map-emitter/continuous-tree-output->map-data data)
                        :discrete-tree   (map-emitter/discrete-tree-output->map-data data)
                        :bayes           (map-emitter/bayes-output->map-data data)
                        :timeslicer      (map-emitter/timeslicer-output->map-data data))
        {:keys [x1 y1 x2 y2]} (get-analysis-objects-view-box analysis-data)
        padding 2]

    {:db (-> db
             (assoc :analysis/data analysis-data))
     :dispatch [:map/set-view-box {:x1 (- x1 padding) :y1 (- y1 padding)
                                   :x2 (+ x2 padding) :y2 (+ y2 padding)}]}))

(defn load-map [_ [_ map-data]]
  {:http-xhrio {:method          :get
                :uri             (:map/url map-data)
                :timeout         8000 
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:map/map-loaded map-data]
                :on-failure      [:log-error]}})

(defn load-data [_ [_ analysis-type analysis-data-url]]
  {:http-xhrio {:method          :get
                :uri             analysis-data-url
                :timeout         8000 
                :response-format (ajax/json-response-format {:keywords? true}) 
                :on-success      [:map/data-loaded analysis-type]
                :on-failure      [:log-error]}})

(defn set-view-box [db [_ {:keys [x1 y1 x2 y2]}]]
  (let [map-screen-width (-> db :map/state :width)
        proj-scale (/ map-screen-width map-proj-width)
        {:keys [translate scale]} (math-utils/calc-zoom-for-view-box x1 y1 x2 y2 proj-scale)]
    (-> db
        (update :map/state merge {:translate translate
                                  :scale     scale}))))

(defn zoom [{:keys [map/state] :as db} [_ x y new-scale]]
  (let [map-screen-width (:width state)
        proj-scale (/ map-screen-width map-proj-width)
        {:keys [translate scale]} state
        screen-coords [x y]
        [proj-x proj-y] (math-utils/screen-coord->proj-coord translate scale proj-scale screen-coords)]
    (update db :map/state
            (fn [{:keys [translate scale] :as map-state}]
              (let [scaled-proj-x (* proj-x scale proj-scale)
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

(defn zoom-inc [{:keys [map/state] :as db} [_ {:keys [delta x y]}]]
  (let [{:keys [translate scale]} state
        zoom-dir (if (pos? delta) -1 1)
        new-scale (+ scale (* zoom-dir 0.8))]
    (zoom db [nil x y new-scale])))

(defn map-grab [db [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map/state :grab :screen-current]  [x y])))

(defn map-release [db _]
  (update db :map/state dissoc :grab))

(defn drag [{:keys [map/state] :as db} [_ {:keys [x y]}]]
  (let [current-screen-coord [x y]]
    (if (:grab state)
      (let [[screen-x screen-y] current-screen-coord
            before-screen-coord (-> state :grab :screen-current)
            [before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db            
            (assoc-in  [:map/state :grab :screen-current] current-screen-coord)
            (update-in [:map/state :translate 0] + screen-drag-x)
            (update-in [:map/state :translate 1] + screen-drag-y)))
      db)))

(defn zoom-rectangle-grab [db [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:map/state :zoom-rectangle] {:origin [x y] :current [x y]})))

(defn zoom-rectangle-update [{:keys [map/state] :as db} [_ {:keys [x y]}]]
  (if (:zoom-rectangle state)
    (assoc-in db [:map/state :zoom-rectangle :current] [x y])
    db))

(defn zoom-rectangle-release [{:keys [db]} _]
  (println "**********" (:map/state db))
  (let [{:keys [map/state]} db
        map-screen-width (:width state)
        proj-scale (/ map-screen-width map-proj-width)
        _ (println "Proj scale is " proj-scale "->" (:width state) "from " state)
        {:keys [translate scale zoom-rectangle]} state
        {:keys [origin current]} zoom-rectangle
        [x1 y1] (math-utils/screen-coord->proj-coord translate scale proj-scale origin)              
        [x2 y2] (math-utils/screen-coord->proj-coord translate scale proj-scale current)]     
    {:db (update db :map/state dissoc :zoom-rectangle)
     :dispatch [:map/set-view-box {:x1 x1 :y1 y1
                                   :x2 x2 :y2 y2}]}))

(defn toggle-show-world [{:keys [db]} _]
  {:db (update-in db [:map/state :show-world?] not)})

(defn download-current-as-svg [{:keys [db]} [_ ]]
  {:spread/download-current-map-as-svg {:geo-json-map (subs/geo-json-data-map (:maps/data db))
                                        :analysis-data (:analysis/data db)
                                        :time (:animation/percentage db)}})

(def tick-step 0.02)

(defn ticker-tick [{:keys [db]} _]
  (let [{:keys [animation/percentage]} db]
    (if (>= (+ percentage tick-step) 1)
      {:db (assoc db :animation/percentage 1)
       :ticker/stop nil}
      
      {:db (update db :animation/percentage #(+ % tick-step))})))

(defn animation-prev [{:keys [animation/percentage] :as db} _]
  (if (<= (- percentage tick-step) 0)
    (assoc db :animation/percentage 0)
    (update db :animation/percentage #(- % tick-step))))

(defn animation-next [{:keys [animation/percentage] :as db} _]
  (if (>= (- percentage tick-step) 1)
    (assoc db :animation/percentage 1)
    (update db :animation/percentage #(+ % tick-step))))

(defn animation-toggle-play-stop [{:keys [db]} _]
  (if (= (:animation/state db) :play)
    {:db (assoc db :animation/state :stop)
     :ticker/stop nil}
    {:db (assoc db :animation/state :play)
     :ticker/start {:millis 50}}))

(defn set-dimensions [db [_ {:keys [width height]}]]
  (println "Reseting map dimensions to " width "x" height)
  (-> db
      (assoc-in [:map/state :width] width)
      (assoc-in [:map/state :height] height)))
