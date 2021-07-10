(ns analysis-viewer.map-emitter
  "
  This namespace contains functionality to parse
  analysis output formats for (continuouse-tree, discrete-tree, bayes and timeslicer)
  and emit a data format suitable for the animated map component.

  It also renames some concepts :

  Output format - Analysis viewer

  Points        -> Nodes
  Counts        -> Circles
  Lines         -> Transitions
  Areas         -> Polygons 
  
  Emited data is in the form of :

  {1 {:type :transition
      :from-coord [10 100]
      :to-coord [115 20]
      :attrs {}
      :show-start 0.2
      :show-end 0.3}
   2 {:type :node
      :coord [123 170]
      :attrs {}
      :show-start 0.2
      :show-end 0.3}
   3 {:type :polygon
      :coords [[10 30] [40 80] [100 100]]
      :attrs {}
      :show-start 0.2
      :show-end 0.3}
    ...}
  

  all coordinates are in proj-coord [x y] where 0 <= x  <= 360, 0 <= y <= 180
  "
  (:require [goog.string :as gstr]
            [shared.math-utils :as math-utils]))

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

(defn index-objects [objs]
  (->> objs
       (map-indexed (fn [idx o]
                      (let [id (str "object-" idx)]
                        [id (assoc o :id id)])))
       (into {})))

(defn continuous-tree-output->map-data [{:keys [timeline points lines areas]}]  
  (let [calc-show-percs (build-show-percentages-calculator timeline)
        points-index (build-points-index points)
        nodes-objects (->> points
                           (map (fn [{:keys [coordinate attributes] :as point}]
                                  (merge
                                   {:type :node
                                    :coord (calc-proj-coord coordinate)                                      
                                    :attrs attributes}
                                   (calc-show-percs point)))))
        transitions-objects (->> lines
                                 (map (fn [{:keys [startPointId endPointId attributes] :as line}]
                                        (let [start-point (get points-index startPointId)
                                              end-point (get points-index endPointId)]
                                          (merge
                                           {:type :transition
                                            :from-coord (calc-proj-coord (:coordinate start-point))
                                            :to-coord (calc-proj-coord (:coordinate end-point))
                                            :attrs attributes}
                                           (calc-show-percs line))))))
        polygons-objects (->> areas
                              (map (fn [{:keys [polygon] :as area}]
                                     (merge
                                      {:type :polygon
                                       :coords (->> (:coordinates polygon)
                                                    (mapv (fn [poly-point]
                                                            (calc-proj-coord poly-point))))
                                       :attrs {}}
                                      (calc-show-percs area)))))        
        objects (concat polygons-objects transitions-objects nodes-objects)]
    (println timeline)
    (println (gstr/format "Continuous tree, got %d nodes, %d transitions, %d polygons"
                          (count nodes-objects)
                          (count transitions-objects)
                          (count polygons-objects)))
    (index-objects objects)))

(defn discrete-tree-output->map-data [{:keys [timeline locations points lines counts]}]
  (let [calc-show-percs (build-show-percentages-calculator timeline)         
        locations-index (->> locations
                             (map (fn [l] [(:id l) l]))
                             (into {}))
        points-index (build-points-index (concat points counts))        
        point-location (fn [point-id]
                         (->> (get points-index point-id)
                              :locationId
                              (get locations-index)))
        nodes-objects (->> points                           
                           (map (fn [{:keys [id attributes] :as point}]
                                  (let [{:keys [coordinate id]} (point-location id)]
                                    (cond-> (merge
                                             {:type :node
                                              :coord (calc-proj-coord coordinate)                                      
                                              :attrs attributes
                                              :label id}
                                             (calc-show-percs point)))))))
        circles-objects (->> counts
                             (map (fn [{:keys [id attributes] :as point}]
                                    (let [{:keys [coordinate id]} (point-location id)]
                                      (cond-> (merge
                                               {:type :circle
                                                :label id
                                                :coord (calc-proj-coord coordinate)                                      
                                                :attrs attributes
                                                :count-attr (get attributes :count)}
                                               (calc-show-percs point))))))) 
        transitions-objects (->> lines
                                 (map (fn [{:keys [startPointId endPointId attributes] :as line}]
                                        (let [start-point (get points-index startPointId)
                                              end-point (get points-index endPointId)
                                              start-loc (point-location (:id start-point))
                                              end-loc   (point-location (:id end-point))]
                                          (merge
                                           {:type :transition
                                            :from-coord (calc-proj-coord (:coordinate start-loc))
                                            :to-coord (calc-proj-coord (:coordinate end-loc))
                                            :from-label (:id start-loc)
                                            :to-label   (:id end-loc)
                                            :attrs attributes}
                                           (calc-show-percs line))))))
        objects (concat transitions-objects nodes-objects circles-objects)]
    (println timeline)
    (println (gstr/format "Discrete tree, got %d nodes, %d transitions, %d circles"
                          (count nodes-objects)
                          (count transitions-objects)
                          (count circles-objects)))
    (index-objects objects)))

(defn bayes-output->map-data [{:keys [locations points lines]}]
  (let [locations-index (->> locations
                             (map (fn [l] [(:id l) l]))
                             (into {}))
        points-index (build-points-index points)        
        point-coordinate (fn [point-id]
                           (->> (get points-index point-id)
                                :locationId
                                (get locations-index)
                                :coordinate))
        nodes-objects (->> points
                           (map (fn [{:keys [id attributes]}]
                                  (let [coordinate (point-coordinate id)]
                                    {:type :node
                                     :show-start 0
                                     :show-end 1
                                     :coord (calc-proj-coord coordinate)                                      
                                     :attrs attributes}))))
        transitions-objects (->> lines
                                 (map (fn [{:keys [startPointId endPointId attributes]}]
                                        (let [start-point (get points-index startPointId)
                                              end-point (get points-index endPointId)]
                                          {:type :transition
                                           :show-start 0
                                           :show-end 1
                                           :from-coord (calc-proj-coord (point-coordinate (:id start-point)))
                                           :to-coord (calc-proj-coord (point-coordinate (:id end-point)))
                                           :attrs attributes}))))
        objects (concat transitions-objects nodes-objects)]
    (println (gstr/format "Bayes analysis, got %d nodes, %d transitions"
                          (count nodes-objects)
                          (count transitions-objects)))
    (index-objects objects)))
  
(defn data-box [data-objects]  
  (let [all-coords (->> data-objects
                        (mapcat (fn [o]
                                  (case (:type o)
                                    :node [(:coord o)]
                                    :circle [(:coord o)]
                                    :polygon (:coords o)
                                    :transition [(:from-coord o) (:to-coord o)])))
                        (into #{}))]
    (math-utils/bounding-box all-coords)))
