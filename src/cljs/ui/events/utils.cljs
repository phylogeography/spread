(ns ui.events.utils
  (:require [taoensso.timbre :as log]))

(defn app-db [{:keys [db]}]
  (log/debug db))
