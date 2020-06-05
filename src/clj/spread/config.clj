(ns spread.config
  (:require [mount.core :as mount :refer [defstate]]
            [spread.utils :as utils]))

(def default
  {:logging {:level :debug
             :console? true}
   :api {:host "127.0.0.1"
         :port 3001
         :queue-name "spread"}
   :messaging {:queues ["spread"]
               :host "127.0.0.1"
               :port 5672
               :username "guest"
               :password "guest"}
   :workers {:workers-count 3
             :queue-name "spread"}})

(defn load! [path]
  (utils/deep-merge default (read-string (slurp path))))

(defstate config
  :start (load! (:config (mount/args))))
