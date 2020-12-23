(ns aws.utils
  (:require [clojure.string :as string]))

(defn throw-on-error
  "Checks aws/invoke response, if it contains a error, throw
  a exception with the error, if it is ok, return the response."
  [resp info-map]
  (if (get resp :cognitect.anomalies/category)
    (throw (ex-info "A error was returned by aws/invoke " {:error-resp resp
                                                           :info info-map}))
    resp))

(defn s3-url->id
  "Returns the id if the url is in proper format"
  [url user-id]
  (let [[_ id-path] (string/split url (re-pattern user-id))
        [id _] (-> id-path
                   (string/replace "/" "")
                   (string/split (re-pattern "\\.")))]
    (if (= (count id) 36)
      id
      (let [message (str "Invalid S3 url " url)]
        (throw (Exception. message))))))
