(ns ui.format)

(def ^:dynamic *default-locale* "en-US")

(def ^:dynamic *default-max-number-fraction-digits* 2)

(defn format-number [x & [{:keys [:locale :max-fraction-digits :min-fraction-digits]
                           :or   {locale              *default-locale*
                                  max-fraction-digits *default-max-number-fraction-digits*}}]]
  (when x
    (.toLocaleString x locale #js {:maximumFractionDigits max-fraction-digits
                                   :minimumFractionDigits min-fraction-digits})))

(defn format-percentage
  "(format/format-percentage 1 3)
  ;; => 33.3%
  (format/format-percentage 1 7 {:max-fraction-digits 4})
  ;; => 14.2857%"
  [portion total & [format-opts]]
  (str (if (pos? total)
         (format-number (* (/ portion total) 100.0)
                        (merge
                          {:max-fraction-digits 1
                           :min-fraction-digits 0}
                          format-opts))
         0)
       "%"))
