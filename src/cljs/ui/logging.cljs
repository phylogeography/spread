(ns ui.logging
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre]))

(def ^:private timbre->devtools-level
  {:fatal js/console.error
   :error js/console.error
   :warn  js/console.warn
   :info  js/console.info
   :debug js/console.info
   :trace js/console.trace})

(defn error? [x]
  (instance? js/Error x))

(def devtools-appender
  "Simple js/console appender which avoids pr-str and uses cljs-devtools
  to format output"
  {:enabled?   true
   :async?     false
   :min-level  nil
   :rate-limit nil
   :output-fn  nil
   :fn         (fn [data]
                 (let [{:keys [level ?ns-str ?line vargs_]} data
                       vargs                                (list* (str ?ns-str ":" ?line) (force vargs_))
                       f                                    (timbre->devtools-level level js/console.log)]
                   (.apply f js/console (to-array vargs))))})

(defn- decode-vargs [vargs]
  (reduce (fn [m arg]
            (assoc m (cond
                       (qualified-keyword? arg) :log-ns
                       (string? arg)            :message
                       (map? arg)               :meta) arg))
          {}
          vargs))

(defn wrap-decode-vargs
  "Middleware for vargs"
  [data]
  (merge data (decode-vargs (-> data
                                :vargs))))

(defn start [{:keys [level console?]}]
  (timbre/merge-config! {:level      (keyword level)
                         :middleware [wrap-decode-vargs]
                         :appenders  {:console (when console?
                                                 devtools-appender)}}))

(defstate logging :start (start (:logging (mount/args))))
