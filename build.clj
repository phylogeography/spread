(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def version "0.0.1")


(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn- uberjar [app-name uber-file-name]
  (clean nil)

  (b/copy-dir {:src-dirs   ["resources"]         ; copy resources
               :target-dir jar-content})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src/clj/api"]
                  :class-dir jar-content})

  (b/uber {:class-dir jar-content                ; create uber file
           :uber-file uber-file-name
           :basis     basis
           :main      'api.main})                ; here we specify the entry point for uberjar

  (println (format "Uber file created: \"%s\"" uber-file-name)))

(defn api-service [_]
  (let [app-name       "api-service"
        uber-file-name "api-service.jar"]
    (uberjar app-name uber-file-name )))

(defn worker-service [_]
  (let [app-name       "worker-service"
        uber-file-name "worker-service.jar"]
    (uberjar app-name uber-file-name )))
