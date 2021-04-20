(ns analysis-viewer.main
  (:require [analysis-viewer.events]
            [analysis-viewer.fxs]
            [analysis-viewer.views :as views]
            [day8.re-frame.http-fx]
            [re-frame.core :as re-frame]
            [reagent.dom :as rdom]))

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))


(defn ^:dev/after-load start []
  (js/console.log "Starting..." )  
  (re-frame/dispatch-sync [:map/initialize
                           ["http://127.0.0.1:9000/spread-dev-uploads/maps/countries/australiaLow.json"
                            "http://127.0.0.1:9000/spread-dev-uploads/maps/countries/tanzaniaLow.json"
                            "http://127.0.0.1:9000/spread-dev-uploads/maps/world/worldLow.json"]
                           :continuous-tree
                           "http://127.0.0.1:9000/spread-dev-uploads/a1195874-0bbe-4a8c-96f5-14cdf9097e02/3a6cc419-5da4-4d28-8b0d-f74c98a89d6e.json"])
  (rdom/render [views/animated-data-map]
               (.getElementById js/document "app")))

(defn ^:export init []
  (start))

