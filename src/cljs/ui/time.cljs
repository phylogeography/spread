(ns ui.time
  (:require [tick.alpha.api :as t]))

(def date-format "yyyy/MM/dd")

(defn now [] (new js/Date))

(defn string->date [str]
  (new js/Date (js/parseInt str)))

(defn format [^js date-obj & [date-time?]]
  (let [date-instant (t/instant date-obj)
        month        (t/int (t/month date-instant))
        month        (if (< month 10 ) (str "0" month) month)]
    (if date-time?
      (str
        (t/day-of-month date-instant)
        "."
        month
        "."
        (t/int (t/year date-instant))
        " "
        (t/hour date-instant)
        ":"
        (t/minute date-instant))
      (str (t/int (t/year date-instant)) "/" month "/" (t/day-of-month date-instant)))))
