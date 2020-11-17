(ns shared.utils
  (:require [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn get-env-variable
  [var-name & [required?]]
  (let [var-value (System/getenv var-name)]
    (if (and (empty? var-value)
             required?)
      (throw (Exception. (str "MISSING ENV VARIABLE: " var-name " not defined in environment")))
      var-value)))

(defn decode-transit
  "Decodes the string s into a clojure datastructure.
  s is expected to be a transit json encoded string."
  [s]
  (let [in (ByteArrayInputStream. (.getBytes s))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn encode-transit
  "Encodes x into a transit json string."
  [x]
  ;; TODO: Figure out this buffer
  ;; this will probably generate a lot of garbage for the GC
  ;; Creating a global stream isn't going to work for multiple threads
  (let [out (ByteArrayOutputStream. 10000)
        writer (transit/writer out :json)]
    (transit/write writer x)
    (.toString out)))



(comment

  (decode-transit (encode-transit {:a 1}))

  )
