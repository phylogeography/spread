(ns ui.events.splash
  (:require [taoensso.timbre :as log]
            [ui.auth :as auth]
            [ui.events.general :refer [initial-user-data-query]]))

(defn initialize-page [{:keys [localstorage]}]
  ;; if no token or expired stay at page, else navigate to the home page
  (when-let [access-token (:access-token localstorage)]
    (let [{:keys [exp]} (auth/decode-token access-token)]
      ;; NOTE: if the token is not expired navigate to home
      ;; this does nothing to check the token validity
      ;; which is checked on the server
      (when (< (js/Date.now)
               (* 1000 exp))
        {:dispatch [:router/navigate :route/home]}))))

(defn send-google-verification-code [_ [_ code redirect-uri]]
  (log/debug {:code code :uri redirect-uri})
  {:dispatch [:graphql/query {:query
                              "mutation GoogleLogin($googleCode: String!, $redirectUri: String!) {
                                    googleLogin(code: $googleCode, redirectUri: $redirectUri) {
                                      accessToken
                                    }
                                  }"
                              :variables {:googleCode code :redirectUri redirect-uri}}]})

(defn send-login-email [_ [_ email redirect-uri]]
  (log/debug {:email email :uri redirect-uri})
  {:dispatch [:graphql/query {:query
                              "mutation SendLoginEmail($email: String!, $redirectUri: String!) {
                                    sendLoginEmail(email: $email, redirectUri: $redirectUri) {
                                      status
                                    }
                                  }"
                              :variables {:email email :redirectUri redirect-uri}}]})

;; TODO : this event likely should also connect the WS
;; see general/initialize
(defn login-success [{:keys [localstorage]} [_ access-token]]
  (log/debug "login success" {:access-token access-token})
  ;; saves token in browser localstorage and navigates to the home page
  {:localstorage (assoc localstorage :access-token access-token)
   ;; retrieve user initial data and navigate home
   :dispatch-n   [[:graphql/query {:query initial-user-data-query}]
                  [:router/navigate :route/home]]})
