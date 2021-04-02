(ns ui.component.button
  (:require [ui.component.icon :refer [icons]]))

(defn button-with-icon [{:keys [icon on-click class]}]
  [:button {:class    class
            :on-click (fn [event]
                        (on-click event)
                        (.stopPropagation event))}
   [:img {:src icon}]])

(defn button-with-label [{:keys [label on-click class]}]
  [:button {:class    class
            :on-click on-click} label])

(defn button-with-icon-and-label [{:keys [icon label on-click class]}]
  [:button {:class    class
            :on-click on-click}
   [:img {:src icon}] label])

(defn- file-select-handler [{:keys [file-with-meta file-accept-predicate on-file-accepted on-file-rejected]} ]
  (if (file-accept-predicate file-with-meta)
    (let [{:keys [file]}      file-with-meta
          ;; url-reader (js/FileReader.)
          array-buffer-reader (js/FileReader.)]
      (set! (.-onload array-buffer-reader) (fn [e]
                                             (let [data (-> e .-target .-result)]

                                               (prn "@before/file-accepted" data)

                                               (on-file-accepted (merge file-with-meta
                                                                        {:data data})))))
      (.readAsArrayBuffer array-buffer-reader file))
    (when on-file-rejected
      (on-file-rejected))))

(defn button-file-input
  [{:keys [icon label class
           file-accept-predicate on-file-accepted on-file-rejected]
    :or   {file-accept-predicate (constantly true)}}]
  [:div.file-input-button {:class class}
   [:input {:type      :file
            :id        "file-input-button"
            :hidden    true
            :on-change (fn [event]
                         (let [file           (-> event .-target .-files (aget 0))
                               file-with-meta {:file file
                                               :filename (.-name file)
                                               :type (if (empty? (.-type file)) "text/plain charset=utf-8" (.-type file))
                                               :size (.-size file)}
                               _ (prn "@file-with-meta" file-with-meta)]
                           (file-select-handler {:file-with-meta        file-with-meta
                                                 :file-accept-predicate file-accept-predicate
                                                 :on-file-accepted      on-file-accepted
                                                 :on-file-rejected      on-file-rejected})))}]
   [:label {:for "file-input-button"} [:img {:src (if (string? icon)
                                                    icon
                                                    (icons icon))} ]label]])
