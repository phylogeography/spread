(ns tests.integration.continuous-tree-test
  (:require [clojure.test :refer [use-fixtures deftest is testing]]
            [api.db :as db]
            [clojure.string :as string]
            [shared.utils :refer [new-uuid]]
            [taoensso.timbre :as log]
            [api.config :as config]
            [api.models.user :as user-model]))

(use-fixtures :once (fn [f]
                      (let [config (config/load)
                            db (db/init (:db config))
                            [first-name _] (shuffle ["Filip" "Philippe" "Guy" "Juan" "Kasper"])
                            [second-name _] (shuffle ["Fu" "Bar" "Smith" "Doe" "Hoe"])
                            [extension _] (shuffle ["io" "com" "gov"])]
                        #_(user-model/upsert-user db {:id (new-uuid)
                                                    :email (string/lower-case (str first-name "@" second-name "." extension))})
                        (f))))

(deftest continuous-tree-test
  (let [fu :bar]

    (log/debug "wut" {:a "be"})

    (is false)

    ))
