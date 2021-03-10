(ns ui.auth
  (:require ["jsonwebtoken" :as jwt]
            [taoensso.timbre :as log]))

(defn decode-token [token]
  (js->clj (.decode ^js jwt token) :keywordize-keys true))

(defn verify-token [token public-key]
  (.verify ^js jwt token public-key))
