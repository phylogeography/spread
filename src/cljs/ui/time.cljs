(ns ui.time
  (:require [tick.alpha.api :as t]))

(def date-format "yyyy/MM/dd")

(defn now [] (new js/Date))
