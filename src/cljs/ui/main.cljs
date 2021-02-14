(ns ui.main
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [ui.config :as config]
            ui.router.core
            [ui.router.component :refer [router]]
            [mount.core :as mount]
            ui.home.page
            [ui.logging]
            ))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn ^:dev/before-load stop []
  (js/console.log "Stopping..."))

(defn ^:dev/after-load start []
  (let [config (config/load)]
    (js/console.log "Starting..." )

    ;; (router/start (:router config))
    (-> (mount/with-args config)
        (mount/start))

    (rdom/render [router]
                 (.getElementById js/document "app")
                 functional-compiler)))

(defn ^:export init []
  (start))
