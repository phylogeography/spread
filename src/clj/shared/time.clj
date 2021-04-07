(ns shared.time
  (:require [tick.alpha.api :as t]))

(defn now
  "returns the current instant"
  []
  (t/now))

(defn millis
  "given an instant returns a Unix timestamp in milliseconds"
  [instant]
  (inst-ms instant))

(defn minus [instant amount units]
  (t/<< instant (t/new-period amount units)))

(defn from-millis
  "given a Unix timestamp in milliseconds returns a UTC instant"
  [millis]
  (t/instant (t/new-duration (if (string? millis) (bigint millis) millis) :millis)))

(defn instant-breakdown
  "Takes an instant of time and breaks it down into units."
  [t]
  {:day  (t/day-of-week t)
   :month  (t/month t)
   :dd (t/day-of-month t)
   :MM (t/int (t/month t))
   :yyyy (t/int (t/year t))
   :mm (t/minute t)
   :HH (t/hour t)
   :ss (t/second t)})

(comment
  (t/instant (t/new-duration (bigint "1616423858803") :millis)))
