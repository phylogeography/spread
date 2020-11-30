(ns api.mutations
  (:require [aws.s3 :as aws-s3]
            [aws.sqs :as aws-sqs]
            [api.models.continuous-tree :as continuous-tree-model]
            [clojure.string :as string]
            [shared.utils :refer [new-uuid clj->gql]]
            [taoensso.timbre :as log]))

(defn s3-url->id
  "returns the id if the url is in the proper bucket,
  proper user directory, and is a proper uuid"
  [url bucket user-id]
  (let [[_ id-path] (string/split url (re-pattern user-id))
        [id _] (-> id-path
                   (string/replace "/" "")
                   (string/split (re-pattern "\\.")))]
    (if (= (count id) 36)
      id
      (let [message "Invalid S3 url"]
        (log/error message {:url url
                            :bucket bucket
                            :user-id user-id})
        (throw (Exception. message))))))

(defn get-upload-urls
  [{:keys [s3-presigner authed-user-id bucket-name]} {:keys [files] :as args} _]
  (log/info "get-upload-urls" {:user/id authed-user-id :files files})
  (loop [files files
         urls []]
    (if-let [file (first files)]
      (let [{:keys [extension]} file
            uuid (new-uuid)]
        (recur (rest files)
               (conj urls (aws-s3/get-signed-url
                           s3-presigner
                           {:bucket-name bucket-name
                            :key (str authed-user-id "/" uuid "." extension)}))))
      urls)))

                                        ; TODO
(defn upload-continuous-tree [{:keys [bucket-name authed-user-id db] :as ctx} {tree-file-url :treeFileUrl
                                                                               :as args} _]
  (log/info "upload-continuous-tree" {:user/id authed-user-id :tree-file-url tree-file-url})
  (let [tree-id (s3-url->id tree-file-url bucket-name authed-user-id)
        continuous-tree {:tree-id tree-id
                         :user-id authed-user-id
                         :tree-file-url tree-file-url}]

    (log/info "@@@ upload-continuous-tree response" (clj->gql continuous-tree))

    ;; TODO : RDS persistance
    (continuous-tree-model/upsert-tree db continuous-tree)

    ;; TODO : to graphql middleware
    (clj->gql continuous-tree)))

;; TODO : message schema
;; TODO : invoke parser
(defn start-parser-execution
  [{:keys [sqs workers-queue-url]} args _]
  (log/info "start-parser-execution" {:a args})
  (aws-sqs/send-message sqs workers-queue-url {:tree "s3://bla/bla"})
  {:id "ffffffff-ffff-ffff-ffff-ffffffffffff"
   :status :QUEUED})


(comment
  (s3-url->id "http://127.0.0.1:9000/minio/spread-dev-uploads/ffffffff-ffff-ffff-ffff-ffffffffffff/3eef35e9-f554-4032-89d3-deb347acd118.tre"
              "spread-dev-uploads"
              "ffffffff-ffff-ffff-ffff-ffffffffffff"))
