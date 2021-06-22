(ns ui.logging
  (:require #_["@sentry/browser" :as Sentry]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre]))

(def ^:private timbre->devtools-level
  {:fatal js/console.error,
   :error js/console.error,
   :warn  js/console.warn,
   :info  js/console.info,
   :debug js/console.info,
   :trace js/console.trace})

#_(def ^:private timbre->sentry-levels
  {:fatal  "fatal"
   :error  "error"
   :warn   "warning"
   :info   "info"
   :debug  "debug"
   :trace  "debug"
   :report "info"})

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


#_(defn sentry-appender [{:keys [min-level] :as config}]
  (.init Sentry (clj->js config))
  {:enabled?   true
   :async?     true
   :min-level  (or min-level :warn)
   :rate-limit nil
   :output-fn  :inherit
   :fn         (fn [{:keys [level ?ns-str ?line message meta log-ns]}]
                 (let [{:keys [error user ns line]} meta]
                   (when meta
                     (.configureScope Sentry
                                      (fn [scope]
                                        (doseq [[k v] meta]
                                          (.setExtra ^js scope (name k) (clj->js v)))
                                        (when user
                                          (.setUser ^js scope (clj->js user))))))
                   (if (error? error) ;; check for js/Error to avoid syntheticException, see https://docs.sentry.io/platforms/node/#hints-for-events
                     (.captureException Sentry error)
                     (.captureEvent Sentry  (clj->js {:level   (timbre->sentry-levels level)
                                                      :message (or message error)
                                                      :logger  (str (or log-ns ns ?ns-str) ":" (or line ?line))})))))})

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

(defn start [{:keys [:level :console? #_:sentry]}]
  (timbre/merge-config! {:level      (keyword level)
                         :middleware [wrap-decode-vargs]
                         :appenders  {:console (when console?
                                                 devtools-appender)
                                      #_#_:sentry  (when sentry
                                                 (sentry-appender sentry))}}))

(defstate logging :start (start (:logging (mount/args))))
