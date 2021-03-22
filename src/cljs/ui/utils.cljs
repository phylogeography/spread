(ns ui.utils
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]))

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

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
