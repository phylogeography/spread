(ns ui.component.button
  (:require [shared.components :refer [button]]))

(defn- file-select-handler [{:keys [file-with-meta file-accept-predicate on-file-accepted on-file-rejected]}]
  (-> (file-accept-predicate file-with-meta)
      (.then (fn [ok?]
               (if ok?
                 (let [{:keys [file]}      file-with-meta
                       array-buffer-reader (js/FileReader.)]
                   (set! (.-onload array-buffer-reader) (fn [^js e]
                                                          (let [data (-> e .-target .-result)]
                                                            (on-file-accepted (merge file-with-meta
                                                                                     {:data data})))))
                   (.readAsArrayBuffer array-buffer-reader file))
                 (if on-file-rejected
                   (on-file-rejected)
                   (js/console.error (str (-> file-with-meta :filename) " rejected"))))))))

(defn button-file-upload
  "`file-accept-predicate` should be a fn that expects a map with :file, :filename, :type, :size and
  returns a js/Promise that resolves to true/false."
  [{:keys [id
           disabled?
           label
           file-accept-predicate
           on-file-accepted
           on-file-rejected]
    :or   {file-accept-predicate (constantly true)}}]
  [:div.file-upload-button
   [:input {:type      :file
            :disabled  disabled?
            :id        (or id "file-upload-button")
            :hidden    true
            :on-change (fn [^js event]
                         (let [file           (-> event .-target .-files (aget 0))
                               file-with-meta {:file     file
                                               :filename (.-name file)
                                               :type     (if (empty? (.-type file))
                                                           "text/plain charset=utf-8"
                                                           (.-type file))
                                               :size     (.-size file)}]
                           (file-select-handler {:file-with-meta        file-with-meta
                                                 :file-accept-predicate file-accept-predicate
                                                 :on-file-accepted      on-file-accepted
                                                 :on-file-rejected      on-file-rejected})))}]
   [:label {:for (or id "file-upload-button")}
    [button {:text label
             :icon "icons/icn_upload_white.svg"
             :class "primary"}]]])
