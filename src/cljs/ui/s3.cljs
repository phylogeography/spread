(ns ui.s3
  (:require [clojure.string :as string]
            [taoensso.timbre :as log]))

(defn upload
  "upload-progress is as optional function taking three arguments: total, sent and percentage"
  [{:keys [url data on-success on-error handle-progress]}]
  (let [^js xhr (new js/XMLHttpRequest)]
    (js/Promise.
      (fn [resolve reject]
        (set! (.-onreadystatechange xhr)
              (fn [] (when (= 4 (.-readyState xhr))
                       (if (= 200 (.-status xhr))
                         (do
                           (when on-success
                             (on-success))
                           (resolve (string/replace (.getResponseHeader xhr "ETAG") #"\"" "")))
                         (do
                           (if on-error
                             (on-error)
                             (log/error "Error during upload" {:xhr  (-> xhr
                                                                        js/JSON.stringify
                                                                        js/JSON.parse)
                                                               :url  url
                                                               :data data}))
                           (reject false))))))
        (.open xhr "PUT" url)
        (.setRequestHeader xhr "Content-Type" (:type data))
        (when handle-progress
          (.addEventListener (.-upload xhr) "progress" (fn [response]
                                                         (let [total (aget response "total")
                                                               sent  (aget response "loaded")]
                                                           (handle-progress sent total)))))
        (.send xhr (clj->js data))))))
