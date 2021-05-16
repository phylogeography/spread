(ns ui.component.button
  (:require [ui.component.icon :refer [arg->icon]]))

(defn button-with-icon [{:keys [icon on-click class disabled?]}]
  [:button {:class    class
            :disabled disabled?
            :on-click (fn [event]
                        (on-click event)
                        (.stopPropagation event))}
   [:img {:src (arg->icon icon)}]])

(defn button-with-label [{:keys [label on-click class disabled?]}]
  [:button {:class    class
            :disabled disabled?
            :on-click on-click} label])

(defn button-with-icon-and-label [{:keys [icon label on-click class disabled?]}]
  [:button {:class    class
            :disabled disabled?
            :on-click on-click}
   [:img {:src (arg->icon icon)}] label])

(defn- file-select-handler [{:keys [file-with-meta file-accept-predicate on-file-accepted on-file-rejected]} ]
  (if (file-accept-predicate file-with-meta)
    (let [{:keys [file]}      file-with-meta
          array-buffer-reader (js/FileReader.)]
      (set! (.-onload array-buffer-reader) (fn [e]
                                             (let [data (-> e .-target .-result)]
                                               (on-file-accepted (merge file-with-meta
                                                                        {:data data})))))
      (.readAsArrayBuffer array-buffer-reader file))
    (when on-file-rejected
      (on-file-rejected))))

;; TODO : https://xd.adobe.com/view/cab84bb6-15c6-44e3-9458-2ff4af17c238-9feb/screen/d09a0797-fbb6-4a0a-891a-21ee253fb709/
(defn button-file-upload
  [{:keys [id
           style
           disabled?
           icon label class
           file-accept-predicate on-file-accepted on-file-rejected]
    :or   {file-accept-predicate (constantly true)}}]
  [:div.file-upload-button {:class [class (when disabled? "disabled")]}
   [:input {:type      :file
            :disabled disabled?
            :id        (or id "file-upload-button")
            :hidden    true
            :on-change (fn [event]
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
   [:label {:style style :for (or id "file-upload-button")} [:img {:src (arg->icon icon)}] label]])
