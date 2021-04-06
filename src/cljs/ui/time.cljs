(ns ui.time
  (:require [tick.alpha.api :as t]))

(def date-format "yyyy/MM/dd")

(defn now [] (new js/Date))

(defn format [^js date-obj]
  (let [date-instant (t/instant date-obj)
        month (t/int (t/month date-instant))
        month (if (< month 10 ) (str "0" month) month)]
    (str (t/int (t/year date-instant)) "/" month "/" (t/day-of-month date-instant))))
