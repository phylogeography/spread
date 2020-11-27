(ns aws.s3
  (:require [aws.utils :refer [throw-on-error]]
            [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :as log])
  (:import (software.amazon.awssdk.services.s3.model PutObjectRequest)
           (software.amazon.awssdk.services.s3.presigner.model PutObjectPresignRequest)
           (software.amazon.awssdk.services.s3.presigner S3Presigner)
           (software.amazon.awssdk.auth.credentials AwsBasicCredentials)
           (software.amazon.awssdk.auth.credentials StaticCredentialsProvider)
           (software.amazon.awssdk.regions Region)
           (java.time Duration)
           (java.net URI)
           ))

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


(defn get-signed-url [s3 {:keys [bucket-name key expires]
                          :or {expires 300}}]

  ;; // put
  ;; GetObjectRequest getObjectRequest =
  ;;               GetObjectRequest.builder()
  ;;                       .bucket(bucketName)
  ;;                       .key(keyName)
  ;;                       .build();

  ;;  // Create a PutObjectPresignRequest to specify the signature duration
  ;;  PutObjectPresignRequest putObjectPresignRequest =
  ;;      PutObjectPresignRequest.builder()
  ;;                             .signatureDuration(Duration.ofMinutes(10))
  ;;                             .putObjectRequest(request)
  ;;                             .build();

        ;; S3Presigner presigner = S3Presigner.create();

  ;; // Generate the presigned request
  ;;  PresignedPutObjectRequest presignedPutObjectRequest =
  ;;      presigner.presignPutObject(putObjectPresignRequest);

  ;; System.out.println("Presigned URL: " + presignedGetObjectRequest.url());

  (let [putObjectRequest (-> (PutObjectRequest/builder)
                             (.bucket bucket-name)
                             (.key key)
                             (.build))

        duration (-> (Duration/ofMinutes 15))

        putObjectPresignRequest (-> (PutObjectPresignRequest/builder)
                                    (.signatureDuration duration)
                                    (.putObjectRequest putObjectRequest)
                                    (.build))

        credentials (AwsBasicCredentials/create "AKIAIOSFODNN7EXAMPLE" "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")

        uri (URI/create "http://127.0.0.1:9000")

        credentialsProvider (StaticCredentialsProvider/create credentials)

        presigner (-> (S3Presigner/builder)
                      (.region (Region/of "us-east-2"))
                      (.endpointOverride uri)
                      (.credentialsProvider credentialsProvider)
                      (.build))

        presignedPutObjectRequest (-> presigner
                                      (.presignPutObject putObjectPresignRequest))

        url (-> presignedPutObjectRequest (.url))

        ]

    (log/debug "@@@ SIGNED-URL" {:url url
                                 :presigner presigner
                                 :uri uri
                                 :credentials credentials
                                 :credentialsProvider credentialsProvider
                                 :presignedPutObjectRequest presignedPutObjectRequest
                                 ;; :duration duration
                                 ;; :presigned-url putObjectRequest
                                 ;; :putObjectPresignRequest putObjectPresignRequest
                                 }))


;; https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/main/java/software/amazon/awssdk/services/s3/presigner/S3Presigner.java

;; https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html

  ;; For example, to configure the Amazon EC2 client to use the Europe (Ireland) Region, use the following code.



  #_(aws/invoke s3

              {:op            :ListBuckets
               :workflow      :cognitect.aws.alpha.workflow/presigned-url
               :presigned-url {:expires 300}}

              #_{:op            :GetSignedUrl ;;PutObject
               ;; :request       {:Bucket bucket-name
               ;;                 :Key key}
               :workflow      :cognitect.aws.alpha.workflow/presigned-url
               :presigned-url {:expires 300
                               :Bucket bucket-name
                               :Key key}}


              #_{:op :fu :request {:Bucket bucket-name
                                           :method "putObject"
                                           :Key key
                                           :Expires expires}})

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
