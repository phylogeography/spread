(ns shared.output-data)

(defn data-bounding-box [output-data]
  (let [point-attrs (:pointAttributes output-data)
        find-attr (fn [attr-id]
                    (some #(when (= (:id %) attr-id) %) point-attrs))                                          
        [min-lon max-lon] (:range (find-attr "xCoordinate"))
        [min-lat max-lat] (:range (find-attr "yCoordinate"))]
    {:min-x min-lon :max-x max-lon :min-y min-lat :max-y max-lat}))
