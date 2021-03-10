(ns ui.splash.events
  (:require [taoensso.timbre :as log]
            [ui.graphql :as graphql]
            [ui.auth :as auth]
            [ui.router.events :as router-events]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  ::initialize-page
  [(re-frame/inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]}]
    ;; if no token or expired stay at page, else navigate to the home page
    (if-let [access-token (:access-token localstorage)]
      (let [{:keys [exp]} (auth/decode-token access-token)]
        ;; NOTE: if the token is not expired navigate to home
        ;; this does nothing to check the token validity
        ;; which is checked on the server
        (when (< (js/Date.now)
                 (* 1000 exp))
          {:dispatch [::router-events/navigate :route/home]})))))

(re-frame/reg-event-fx
  ::send-google-verification-code
  (fn [{:keys [db]} [_ code redirect-uri]]
    (log/debug {:code code :uri redirect-uri})
    {:dispatch [::graphql/query {:query
                                 "mutation GoogleLogin($googleCode: String!, $redirectUri: String!) {
                                    googleLogin(code: $googleCode, redirectUri: $redirectUri) {
                                      accessToken
                                    }
                                  }"
                                 :variables {:googleCode code :redirectUri redirect-uri}}]}))

(re-frame/reg-event-fx
  :splash/login-success
  [(re-frame/inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [_ access-token]]
    (log/debug "login success" {:access-token access-token})
    ;; saves token in browser localstorage and navigates to the home page
    {:localstorage (assoc localstorage :access-token access-token)
     :dispatch     [::router-events/navigate :route/home]}))
