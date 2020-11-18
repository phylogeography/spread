(ns aws.utils)

(defn throw-on-error
  "Checks aws/invoke response, if it contains a error, throw
  a exception with the error, if it is ok, return the response."
  [resp info-map]
  (if (get resp :cognitect.anomalies/category)

    (throw (ex-info "A error was returned by aws/invoke " {:error-resp resp
                                                           :info info-map}))

    resp))
