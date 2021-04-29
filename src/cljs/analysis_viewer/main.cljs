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

;; Some continuous tree examples

;; http://localhost:8021/?output-type=continuous-tree&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/644e6959-0a6c-4da8-8413-cb4caafe5f62.json&maps=AU,TZ
;; http://localhost:8021/?maps=AU,TZ&output-type=continuous-tree&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/3a6cc419-5da4-4d28-8b0d-f74c98a89d6e.json
;; http://localhost:8021/?output-type=continuous-tree&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/3a6cc419-5da4-4d28-8b0d-f74c98a89d6e.json&maps=AU,TZ

;; Some discrete tree examples

;; http://localhost:8021/?output-type=discrete-tree&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/985ecd9f-a9eb-425d-9795-7e92a81d2941.json&maps=AU,TZ

;; Some bayes examples

;; http://localhost:8021/?output-type=bayes&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/e9d1f428-0d26-42a0-bb7a-b9c14e59d9dd.json&maps=AU,TZ

;; Some timeslicer examples

;; http://localhost:8021/?output-type=timeslicer&output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/8d76e55a-441b-4e65-8691-7e664b70e5bf.json&maps=AU,TZ

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
  (let [{:keys [maps output output-type]} (parse-url-qstring (subs js/window.location.search 1))]
    (re-frame/dispatch-sync [:map/initialize
                             (into ["WORLD"] (str/split maps #",") )
                             (keyword output-type)
                             (str events.maps/s3-bucket-url "/" output)])
    (rdom/render [views/main-screen]
                 (.getElementById js/document "app"))))

(defn ^:export init []
  (start))

