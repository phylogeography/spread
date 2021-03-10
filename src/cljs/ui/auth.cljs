(ns ui.auth
  (:require ["jsonwebtoken" :as jwt]))

(defn decode-token [token]
  (js->clj (.decode ^js jwt token) :keywordize-keys true))

(defn verify-token [token public-key]
  (.verify ^js jwt token public-key))
