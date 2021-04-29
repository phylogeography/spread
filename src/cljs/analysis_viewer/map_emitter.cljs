(ns analysis-viewer.map-emitter
  "
  This namespace contains functionality to parse
  analysis output formats for (continuouse-tree, discrete-tree, bayes and timeslicer)
  and emit a data format suitable for the animated map component.

  Emited data is in the form of :

  [{:type :arc
    :from-coord [10 100]
    :to-coord [115 20]
    :attrs {}
    :show-start 0.2
    :show-end 0.3}
   {:type :point
    :coord [123 170]
    :attrs {}
    :show-start 0.2
    :show-end 0.3}
   {:type :area
    :coords [[10 30] [40 80] [100 100]]
    :attrs {}
    :show-start 0.2
    :show-end 0.3}
   ...]

  all coordinates are in proj-coord [x y] where 0 <= x  <= 360, 0 <= y <= 180
  "
  (:require [shared.math-utils :as math-utils]
            [goog.string :as gstr]))

(defn build-show-percentages-calculator [timeline]
  (let [{:keys [startTime endTime]} timeline
        start-time-millis (.getTime (js/Date. startTime))
        end-time-millis (.getTime (js/Date. endTime))
        timeline-millis (- end-time-millis start-time-millis)]
    (fn [{:keys [startTime endTime]}]
      (let [p-start-time-millis (if startTime
                                  (.getTime (js/Date. startTime))
                                  start-time-millis)
            p-end-time-millis (if endTime
                                (.getTime (js/Date. endTime))
                                end-time-millis)]
        {:show-start (/ (- p-start-time-millis start-time-millis) timeline-millis)
         :show-end   (/ (- p-end-time-millis start-time-millis) timeline-millis)}))))

(defn calc-proj-coord [{:keys [xCoordinate yCoordinate]}]
  (math-utils/map-coord->proj-coord [xCoordinate yCoordinate]))

(defn build-points-index [points]
  (->> points
       (map (fn [p] [(:id p) p]))
       (into {})))

(defn continuous-tree-output->map-data [{:keys [timeline layers]}]  
  (let [layer (first layers) ;; current that format only use one layer
        calc-show-percs (build-show-percentages-calculator timeline)
        points-index (build-points-index (:points layer))
        points-objects (->> (:points layer)
                            (map (fn [{:keys [coordinate attributes] :as point}]
                                   (merge
                                    {:type :point
                                     :coord (calc-proj-coord coordinate)                                      
                                     :attrs attributes}
                                    (calc-show-percs point)))))
        arcs-objects (->> (:lines layer)
                           (map (fn [{:keys [startPointId endPointId attributes] :as line}]
                                  (let [start-point (get points-index startPointId)
                                        end-point (get points-index endPointId)]
                                    (merge
                                     {:type :arc
                                      :from-coord (calc-proj-coord (:coordinate start-point))
                                      :to-coord (calc-proj-coord (:coordinate end-point))
                                      :attrs attributes}
                                     (calc-show-percs line))))))
        area-objects (->> (:areas layer)
                          (map (fn [{:keys [polygon] :as area}]
                                 (merge
                                  {:type :area
                                   :coords (->> (:coordinates polygon)
                                                (mapv (fn [poly-point]
                                                        (calc-proj-coord poly-point))))
                                   :attrs {}}
                                  (calc-show-percs area)))))        
        objects (->> (concat area-objects arcs-objects points-objects)
                     (map-indexed (fn [idx o]
                                    (assoc o :id idx))))]
    (println timeline)
    (println (gstr/format "Continuous tree, got %d points, %d arcs, %d areas" (count points-objects) (count arcs-objects) (count area-objects)))
    objects))

(defn discrete-tree-output->map-data [{:keys [timeline axisAttributes lineAttributes pointAttributes locations layers] :as data}]
  
  (let [[counts-layer tree-layer] layers
        calc-show-percs (build-show-percentages-calculator timeline)
        all-points (concat (:points counts-layer) (:points tree-layer))
        locations-index (->> locations
                             (map (fn [l] [(:id l) l]))
                             (into {}))
        points-index (build-points-index all-points)        
        point-coordinate (fn [point-id]
                           (->> (get points-index point-id)
                                :locationId
                                (get locations-index)
                                :coordinate))
        points-objects (->> all-points
                            (map (fn [{:keys [id attributes] :as point}]
                                   (let [coordinate (point-coordinate id)
                                         count-attr (get attributes :count)]
                                     (cond-> (merge
                                              {:type :point
                                               :coord (calc-proj-coord coordinate)                                      
                                               :attrs attributes}
                                              (calc-show-percs point))
                                       count-attr (assoc :radius-factor count-attr))))))
        arcs-objects (->> (:lines tree-layer)
                          (map (fn [{:keys [startPointId endPointId attributes] :as line}]
                                 (let [start-point (get points-index startPointId)
                                       end-point (get points-index endPointId)]
                                   (merge
                                    {:type :arc
                                     :from-coord (calc-proj-coord (point-coordinate (:id start-point)))
                                     :to-coord (calc-proj-coord (point-coordinate (:id end-point)))
                                     :attrs attributes}
                                    (calc-show-percs line))))))
        objects (->> (concat arcs-objects points-objects)
                     (map-indexed (fn [idx o]
                                    (assoc o :id idx))))]
    (println timeline)
    (println (gstr/format "Discrete tree, got %d points, %d arcs" (count points-objects) (count arcs-objects)))
    objects))

(defn bayes-output->map-data [_]  
  ;; TODO: implement
  (throw (js/Error. "Bayes map data emitter not implemented yet.")))

(defn timeslicer-output->map-data [_]
  ;; TODO: implement
  (throw (js/Error. "Timeslicer map data emitter not implemented yet.")))

