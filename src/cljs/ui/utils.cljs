(ns ui.utils
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame])
  (:import [goog.async Debouncer]))

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

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
