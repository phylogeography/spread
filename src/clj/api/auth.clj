(ns api.auth
  (:require [buddy.core.keys :as buddy.keys]
            [buddy.sign.jws :as buddy.sign]
            [buddy.sign.jwt :as jwt]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-http.client :as http]
            [shared.time :as time]
            [shared.utils :refer [decode-json]]))

(defn token-decode-header
  "Decodes a JWT tokens header"
  [token]
  (buddy.sign/decode-header token))

(defn verify-token
  "Verifies a JWT token, automatically verifyes :exp claim
  Parameters:
  :token, the token string
  :public-key, the public key to validate the token
  :claims a map of claims to verify (:iss :aud)"
  [{:keys [token public-key claims]}]
  (jwt/unsign token public-key (merge {:alg :rs256}
                                      claims)))

(defn get-google-public-key
  "Retrieves the google public key used to validate google authentication tokens."
  [kid]
  (-> (http/get "https://www.googleapis.com/oauth2/v1/certs")
      :body
      (decode-json)
      (get (keyword kid))
      buddy.keys/str->public-key))

(defn verify-google-token
  "Verify token with google public key. If the token is valid and not expired
  returns the google token data"
  [token google-client-id]
  (let [header     (token-decode-header token)
        ;; TODO: memoize it for kid (key id)
        public-key (get-google-public-key (:kid header))
        token-data (verify-token {:token      token
                                  :public-key public-key
                                  :claims     {:iss ["https://accounts.google.com" "accounts.google.com"]
                                               :aud google-client-id}})]
    token-data))

(defn token-encode
  "Generates a signed JWT token.
  Parameters:
  :private-key to sign the token
  :claims a map of claims to add
  :expiration, timestamp of token expiration"
  [{:keys [private-key claims]}]
  (jwt/sign claims
            private-key
            {:alg :rs256}))

(defn generate-spread-access-token
  "Generates a long-lived spread token signed with our private key"
  [private-key user]
  (when-not (:id user)
    (throw (ex-info "Missing user id" {})))
  (let [{:keys [id]} user
        now          (/ (time/millis (time/now)) 1000) ;; in seconds
        expires      (+ now 2628e6)                    ;; now + 1 month
        token        (token-encode {:private-key private-key
                                    :claims      {:iss "spread"
                                                  :iat now
                                                  :exp expires
                                                  :aud "spread-client"
                                                  :sub id}})]
    {:access-token token
     :expires-at   expires}))

;; TODO:
;; read token from the bearer header
;; verify token
;; return user-id (or nil)
(defn token->user-id [_]
  "ffffffff-ffff-ffff-ffff-ffffffffffff")

(comment
  (def token "eyJhbGciOiJSUzI1NiIsImtpZCI6ImU4NzMyZGIwNjI4NzUxNTU1NjIxM2I4MGFjYmNmZDA4Y2ZiMzAyYTkiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI4MDYwNTI3NTc2MDUtNXNidWJiazl1YmowdHE5NWRwN2I1OHYzNnRzY3F2MXIuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI4MDYwNTI3NTc2MDUtNXNidWJiazl1YmowdHE5NWRwN2I1OHYzNnRzY3F2MXIuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTMyNTI3ODg4MjExOTc1Mzc4MjUiLCJlbWFpbCI6ImZiaWVsZWplY0BnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6IlhDS3pKUDBWX280blo1MFZBSFZOeUEiLCJuYW1lIjoiRmlsaXAgQmllbGVqZWMiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EtL0FPaDE0R2pXWnlJbWYzbUJHbkhQakpPbEUwSkRZeEFuUGJYR2JiUFhJRmFITnc9czk2LWMiLCJnaXZlbl9uYW1lIjoiRmlsaXAiLCJmYW1pbHlfbmFtZSI6IkJpZWxlamVjIiwibG9jYWxlIjoiZW4iLCJpYXQiOjE2MTUyMDMxNTgsImV4cCI6MTYxNTIwNjc1OH0.raFplypYNiaA0YAeoaiOM4htimIavRdI8zkQJhMwqJv4EMTil3louY-Wtrs2F37s6gVhh7CmABcDKYxeCQeRWZUabpcVJo09AP3m_Ps6tM6-h4kXMyr6wvhNleYFKXC7tV5lW7uIzn3s773P2TP1u7hPvrysYD0G2aDtibyaCU81MO3LoWlhat9LnNpkd-_Ubty_9CVnxjr28NAtEsNZ7W_e2X1jX0WPNuZdrJv9lIP4JAdwlg3AiKEnatyfpj5_psDGkZJbWrSJmKDKoWytUynMOeXGFOe6jzr1P0zhk-rqJBCm5TKVM9uDXkx5MWXyKCNer-wXX0ZC2vhnZEUIGA")
  (verify-google-token token "806052757605-5sbubbk9ubj0tq95dp7b58v36tscqv1r.apps.googleusercontent.com")

  (def keypair (generate-keypair 512))

  (def public-key (.getPublic keypair))


  )
