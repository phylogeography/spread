(ns ui.main
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            ))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn app []
  [:div [:p "Hello"]])

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))

(defn ^:dev/after-load start []
  (js/console.log "Starting..." )
  (rdom/render [app]
               (.getElementById js/document "app")
               functional-compiler))

(defn ^:export init []
  (start))
