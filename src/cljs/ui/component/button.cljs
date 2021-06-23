(ns ui.component.button
  (:require [reagent-material-ui.core.button :refer [button]]
            [reagent.core :as reagent]
            [ui.component.icon :refer [arg->icon]]))

;; (defn button-with-icon [{:keys [icon on-click class disabled?]}]
;;   [:button {:class    class
;;             :disabled disabled?
;;             :on-click (fn [event]
;;                         (on-click event)
;;                         (.stopPropagation event))}
;;    [:img {:src (arg->icon icon)}]])

(defn button-with-label [{:keys [label on-click class disabled?]}]
  [:button {:class    class
            :disabled disabled?
            :on-click on-click} label])

;; (defn button-with-icon-and-label [{:keys [icon label on-click class disabled?]}]
;;   [:button {:class    class
;;             :disabled disabled?
;;             :on-click on-click}
;;    [:img {:src (arg->icon icon)}] label])

(defn- file-select-handler [{:keys [file-with-meta file-accept-predicate on-file-accepted on-file-rejected]}]
  (if (file-accept-predicate file-with-meta)
    (let [{:keys [file]}      file-with-meta
          array-buffer-reader (js/FileReader.)]
      (set! (.-onload array-buffer-reader) (fn [^js e]
                                             (let [data (-> e .-target .-result)]
                                               (on-file-accepted (merge file-with-meta
                                                                        {:data data})))))
      (.readAsArrayBuffer array-buffer-reader file))
    (when on-file-rejected
      (on-file-rejected))))

(defn button-file-upload
  [{:keys [id
           disabled?
           icon
           label
           class-name
           file-accept-predicate
           on-file-accepted
           on-file-rejected]
    :or   {file-accept-predicate (constantly true)}}]
  [:<>
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
    [button {:class-name class-name
             :disabled  disabled?
             :variant    "contained" :color "primary" :component "span"
             :startIcon  (reagent/as-element [:img {:src (arg->icon icon)}])}
     label]]])
