(ns aws.s3
  (:require [aws.utils :refer [throw-on-error]]
            [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :as log]))

(defn create-client

  "Creates a aws S3 client. Needed for calling all s3 functions."

  [{:keys [access-key-id secret-access-key region s3-host s3-port]}]
  (aws/client (cond-> {:api :s3
                       :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id access-key-id
                                               :secret-access-key secret-access-key})}
                region (assoc :region region)
                ;; only for dev
                s3-host (assoc :endpoint-override {:protocol :http
                                                   :hostname s3-host
                                                   :port s3-port}))))

(defn list-buckets [s3]
  (aws/invoke s3 {:op :ListBuckets}))

(defn create-bucket [s3 {:keys [bucket-name]}]
  (aws/invoke s3 {:op :CreateBucket :request {:Bucket bucket-name}}))


(defn get-signed-url [s3 {:keys []}]


  )

(defn upload-file

  "Uploads the file at `file-path` under a s3 `bucket` under `key`."

  [s3 {:keys [bucket key file-path]}]
  (-> (aws/invoke s3 {:op :PutObject
                      :request {:Bucket bucket
                                :Key key
                                :Body (-> file-path
                                          io/file
                                          io/input-stream)}})
      (throw-on-error {:api :s3 :fn ::upload-file})))

(defn download-file

  "Downloads the file from s3 `bucket` and `key` and
  stores it in a file at `dest-path`."

  [s3 bucket key dest-path]
  (log/info "Downloading file from s3" {:bucket bucket
                                        :key key
                                        :saving-in dest-path})
  (-> (aws/invoke s3 {:op :GetObject
                      :request {:Bucket bucket
                                :Key key}})
      (throw-on-error {:api :s3 :fn ::download-file})
      :Body
      (io/copy (io/file dest-path))))

(defn build-url [aws-config bucket key]
  (if-let [endpoint (:s3-endpoint aws-config)]
    ;; this is MinIO
    (format "%s/minio/%s/%s"
            endpoint
            bucket
            key)

    ;; no endpoint means prod/staging/dev
    (format "https://%s.s3.%s.amazonaws.com/%s"
            bucket
            (:region aws-config)
            key)))
