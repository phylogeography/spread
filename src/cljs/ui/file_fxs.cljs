(ns ui.file-fxs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-fx
 :spread/download-text-as-file
 (fn [{:keys [file-name file-type text]}]
   (let [download-anchor (js/document.createElement "a")]
     (.setAttribute download-anchor "href" (str file-type "," (js/encodeURIComponent text)))
     (.setAttribute download-anchor "download" file-name)
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor))))
