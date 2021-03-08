(ns ui.splash.page
  (:require [ui.router.events :as router-events]
            [ui.router.subs :as router.subs]
            [ui.subs :as subs]
            [ui.splash.events :as events]
            [ui.router.component :refer [page]]
            [ui.utils :refer [<sub >evt] :as utils]
            [lambdaisland.uri :refer [uri]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [clojure.string :as string]
            [reagent.core :as r]))

;; https://developers.google.com/identity/protocols/oauth2/javascript-implicit-flow#oauth-2.0-endpoints

;; http://localhost:8020/?auth=google&code=4/0AY0e-g7IKNSpCBeN-1o98OaWz84VxlrRF3IRtlX_1Is1H7XqOuvz2bxIiTr7LXrNfa7j3Q&scope=email%20openid%20https://www.googleapis.com/auth/userinfo.email&authuser=1&hd=clashapp.co&prompt=none

(defmethod page :route/splash []
  (let [{:keys [root-url google]}       (<sub [::subs/config])
        {:keys [query] :as active-page} (<sub [::router.subs/active-page])
        {:keys [client-id redirect-uri]} google
        ;; redirect-uri (utils/url-encode redirect-uri)
        ]
    (r/create-class
      {:display-name        "splash"
       :component-did-mount (fn []
                              (log/debug "component-did-mount" query)
                              (when-let [code (:code query)]
                                (case (-> query :auth keyword)
                                  :google
                                  (>evt [::events/send-google-verification-code code redirect-uri])
                                  nil)))
       :reagent-render
       (fn []
         [:div
          [:a.button {:href
                      (str "https://accounts.google.com/o/oauth2/v2/auth"
                           "?client_id=" client-id
                           "&scope=email%20profile"
                           "&response_type=code"
                           "&redirect_uri=" (utils/url-encode redirect-uri))}
           "Login with Google"]])})))
