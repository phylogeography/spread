(ns ui.utils
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame])
  (:import goog.async.Debouncer))

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

(def type->tab {"CONTINUOUS_TREE"       "continuous-mcc-tree"
                "DISCRETE_TREE"         "discrete-mcc-tree"
                "BAYES_FACTOR_ANALYSIS" "discrete-rates"})

(defn debounce [f millis]
  (let [dbnc (Debouncer. f millis)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn url-encode
  [s]
  (some-> s str (js/encodeURIComponent) (.replace "+" "%20")))

(defn ensure-trailing-slash [s]
  (str s
       (when-not (string/ends-with? s "/")
         "/")))

(defn dissoc-in [m [k & ks]]
  (if ks
    (if (map? (get m k))
      (update m k #(dissoc-in % ks))
      m)
    (dissoc m k)))

(defn concatv [& more]
  (vec (apply concat more)))

(defn split-first [s re]
  (string/split s re 2))

(defn split-last [s re]
  (let [pattern (re-pattern (str re "(?!.*" re ")"))]
    (split-first s pattern)))

(letfn [(merge-in* [a b]
          (if (map? a)
            (merge-with merge-in* a b)
            b))]
  (defn merge-in
    "Merge multiple nested maps."
    [& args]
    (reduce merge-in* nil args)))

(defn dispatch-n [events]
  (when (sequential? events)
    (doseq [event (remove nil? events)]
      (re-frame/dispatch event))))

(defn fully-typed-number
  "Given a number as a string, parse it to float only if it is a fully typed number,
  return nil otherwise. "
  [n-str]
  (when (and n-str (re-matches #"^\d+((\.|\,)\d+)?" n-str))
    (js/parseFloat n-str)))

(def month-name ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Set" "Oct" "Nov" "Dec"])

(defn format-date [timestamp-millis]
  (let [date (js/Date. timestamp-millis)
        year (.getFullYear date)
        month (.getMonth date)
        day (.getDate date)]
    (str day " " (month-name month) " " year)))

(defn round-number
   [f precision]
  (let [c (js/Math.pow 10 precision)]
    (/ (.round js/Math (* c f)) c)))

(defn round [number precision]
  (if (number? number)
    (round-number number precision)
    number))
