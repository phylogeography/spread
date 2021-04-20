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
  (:require [shared.math-utils :as math-utils]))

(defn continuous-tree-output->map-data [{:keys [timeline layers]}]  
  (let [layer (first layers) ;; current that format only use one layer
        {:keys [startTime endTime]} timeline
        start-time-millis (.getTime (js/Date. startTime))
        end-time-millis (.getTime (js/Date. endTime))
        timeline-millis (- end-time-millis start-time-millis)
        calc-show-percs (fn [{:keys [startTime endTime]}]
                          (let [p-start-time-millis (if startTime
                                                      (.getTime (js/Date. startTime))
                                                      start-time-millis)
                                p-end-time-millis (if endTime
                                                    (.getTime (js/Date. endTime))
                                                    end-time-millis)]
                            {:show-start (/ (- p-start-time-millis start-time-millis) timeline-millis)
                             :show-end   (/ (- p-end-time-millis start-time-millis) timeline-millis)}))
        calc-proj-coord (fn [{:keys [xCoordinate yCoordinate]}]
                          (math-utils/map-coord->proj-coord [xCoordinate yCoordinate]))
        points-map (->> (:points layer)
                        (map (fn [p] [(:id p) p]))
                        (into {}))
        points-objects (->> (:points layer)
                            (map (fn [{:keys [coordinate attributes] :as point}]
                                   (merge
                                    {:type :point
                                     :coord (calc-proj-coord coordinate)                                      
                                     :attrs attributes}
                                    (calc-show-percs point)))))
        arcs-objects (->> (:lines layer)
                           (map (fn [{:keys [startPointId endPointId attributes] :as line}]
                                  (let [start-point (get points-map startPointId)
                                        end-point (get points-map endPointId)]
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
                                   :coords (->> polygon
                                                (mapv (fn [poly-point]
                                                        (calc-proj-coord poly-point))))
                                   :attrs {}}
                                  (calc-show-percs area)))))
        objects (->> (concat points-objects arcs-objects area-objects)
                     (map-indexed (fn [idx o]
                                    (assoc o :id idx))))]
    objects))

(defn discrete-tree-output->map-data [_]
  []
  ;; TODO: implement 
  )

(defn bayes-output->map-data [_]
  []
  ;; TODO: implement 
  )

(defn timeslicer-output->map-data [_]
  []
  ;; TODO: implement 
  )

