(ns analysis-viewer.main
  (:require [analysis-viewer.events]
            [analysis-viewer.events.maps :as events.maps]
            [analysis-viewer.fxs]
            [analysis-viewer.views :as views]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [flow-storm.api :as fsa]
            [re-frame.core :as re-frame]
            [re-frame.db]
            [reagent.dom :as rdom]))

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))

;; google-chrome --allow-file-access-from-files file:///home/jmonetta/non-rep-software/spread-d3/language_D3/renderers/d3/d3renderer/index.html


;; http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/644e6959-0a6c-4da8-8413-cb4caafe5f62.json&maps=AU,TZ

;; http://localhost:8021/?maps=AU,TZ&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/3a6cc419-5da4-4d28-8b0d-f74c98a89d6e.json
;; http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/3a6cc419-5da4-4d28-8b0d-f74c98a89d6e.json&maps=AU,TZ

(defn parse-url-qstring [qstring]
  (->> (str/split qstring #"&")
       (map (fn [s]
              (let [[pname pval] (str/split s #"=")]
                [(keyword pname) pval])))
       (into {})))

(defn ^:dev/after-load start []
  (js/console.log "Starting..." )
  (fsa/connect {:tap-name "analysis-viewer"})
  (fsa/trace-ref re-frame.db/app-db {:ref-name "re-frame-db"
                                     :ignore-keys [:maps/data ;; this one is super big
                                                   :map/state ;; this one changes a lot when zooming, dragging, etc
                                                   :animation/percentage ;; this one changes a lot
                                                   ]}) 
  (let [{:keys [maps output]} (parse-url-qstring (subs js/window.location.search 1))]
    (re-frame/dispatch-sync [:map/initialize
                             (into ["WORLD"] (str/split maps #",") )
                             :continuous-tree
                             (str events.maps/s3-bucket-url "/" output)])
    (rdom/render [views/main-screen]
                 (.getElementById js/document "app"))))

(defn ^:export init []
  (start))

