(ns ui.s3
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

(defn upload
  "handle-progress is as optional function taking two arguments: sent and total"
  [{:keys [url data on-success on-error handle-progress]}]

  (prn "@@@ upload" url data)

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

(re-frame/reg-fx
 ::upload
 (fn [params]
   (upload params)))
