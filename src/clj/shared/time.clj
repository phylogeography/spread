(ns shared.time
  (:require
   [tick.alpha.api :as time]))

(defn now
  "returns the current instant"
  []
  (time/now))

(defn millis
  "given an instant returns a Unix timestamp in milliseconds"
  [instant]
  (inst-ms instant))

(defn minus [instant amount units]
  (time/<< instant (time/new-period amount units)))
