(ns ui.events.splash
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [ui.auth :as auth]))

(defn initialize-page [{:keys [localstorage]}]
  ;; if no token or expired stay at page, else navigate to the home page
  (if-let [access-token (:access-token localstorage)]
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

(defn login-success[{:keys [localstorage]} [_ access-token]]
  (log/debug "login success" {:access-token access-token})
  ;; saves token in browser localstorage and navigates to the home page
  {:localstorage (assoc localstorage :access-token access-token)
   :dispatch     [:router/navigate :route/home]})
