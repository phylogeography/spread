(ns ui.splash.events
  (:require [taoensso.timbre :as log]
            [ui.graphql :as graphql]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  ::initialize-page
  (fn []
    (log/debug "splash/initialize-page")))

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
