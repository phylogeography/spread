(ns spread.fxs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-fx
 ::download-current-map-as-svg
 (fn [_]   
   (let [svg-elem (js/document.getElementById "map-and-data")
         svg-text (.-outerHTML svg-elem)
         download-anchor (js/document.createElement "a")]

     (.setAttribute download-anchor "href" (str "data:image/svg+xml;charset=utf-8," (js/encodeURIComponent svg-text)))
     (.setAttribute download-anchor "download" "map.svg")
     (set! (-> download-anchor .-style .-display) "none")
     (js/document.body.appendChild download-anchor)
     (.click download-anchor)
     (js/document.body.removeChild download-anchor)
     (println "Done"))))
