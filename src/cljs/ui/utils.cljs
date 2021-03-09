(ns ui.utils
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]))

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

(defn url-encode
  [string]
  (some-> string str (js/encodeURIComponent) (.replace "+" "%20")))

(defn ensure-trailing-slash [s]
  (str s
       (when-not (string/ends-with? s "/")
         "/")))

(defn reg-empty-event-fx [id]
  (re-frame/reg-event-fx
   id
   (constantly nil)))
