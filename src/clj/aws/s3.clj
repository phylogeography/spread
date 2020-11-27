(ns aws.s3
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [shared.utils :refer [decode-transit encode-transit]]
            [aws.utils :refer [throw-on-error]]))

(defn create-client [{:keys [access-key-id secret-access-key region s3-host s3-port]}]

  (aws/client (cond-> {:api :s3
                       :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id access-key-id
                                               :secret-access-key secret-access-key})}

                region (assoc :region region)

                ;; (nil? region)

                ))

  )
