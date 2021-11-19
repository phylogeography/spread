(ns shared.logging
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre]
            [timbre-json-appender.core :as json]))

(declare logging)

(defn start [config]
  (let [{:keys [level pretty?]} (:logging config)]
    (timbre/merge-config!
     {:level (keyword level)
      :timestamp-opts {:pattern "yyyy-MM-dd'T'HH:mm:ssX"}
      :appenders {:json (json/json-appender {:pretty pretty?})
                  :println false}})))

(defstate logging
  :start (start (mount/args)))
