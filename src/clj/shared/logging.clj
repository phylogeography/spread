(ns shared.logging
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as string]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre]))

(defn- logline [data]
  (-> data
      (select-keys [:level :?ns-str :?file :?line :message :meta :instant])
      (set/rename-keys {:instant :timestamp})))

(defn console-logline [data]
  (let [{:keys [level log-ns ?ns-str ?file ?line message meta timestamp]} (logline data)
        {:keys [ns line file]} meta]
    (string/join " "
                 [(string/upper-case (name level))
                  (str message
                       (when meta
                         (str "\n" (with-out-str (pprint/pprint meta)))))
                  "in"
                  (str
                   (or log-ns ns ?ns-str)
                   "["
                   (or file ?file)
                   ":"
                   (or line ?line)
                   "]")
                  "at"
                  timestamp])))

(defn console-appender []
  {:enabled? true
   :async? false
   :min-level nil
   :rate-limit nil
   :output-fn nil
   :fn (fn [data]
         (print (console-logline data)))})

(defn- decode-vargs [vargs]
  (reduce (fn [m arg]
            (assoc m (cond
                       (qualified-keyword? arg) :log-ns
                       (string? arg) :message
                       (map? arg) :meta) arg))
          {}
          vargs))

(defn wrap-decode-vargs
  "Middleware for vargs"
  [data]
  (merge data (decode-vargs (:vargs data))))

(defn start [config]
  (let [{:keys [level] :as args} (:logging config)]
    (timbre/merge-config!
     {:level (keyword level)
      :middleware [wrap-decode-vargs]
      :appenders {:console (console-appender)}})))

(defstate logging :start (start (mount/args)))
