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
            [reagent.dom :as rdom]
            [clojure.string :as str]))

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))

;; google-chrome --allow-file-access-from-files file:///home/jmonetta/non-rep-software/spread-d3/language_D3/renderers/d3/d3renderer/index.html

;; [CT] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/dab13811-8c99-454b-9fc4-b31d6dfcae9e.json&maps=
;; [B]  http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/ba47f735-0a97-40b1-b761-860b1a914e28.json
;; [TS] http://localhost:8021/?output=a1195874-0bbe-4a8c-96f5-14cdf9097e02/8d7b6ea9-b4f7-4387-9b35-042e5ed89981.json

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
                             (into ["WORLD"] (remove str/blank? (str/split maps #",")) )                             
                             (str events.maps/s3-bucket-url "/" output)])
    (rdom/render [views/main-screen]
                 (.getElementById js/document "app"))))

(defn ^:export init []
  (start))

