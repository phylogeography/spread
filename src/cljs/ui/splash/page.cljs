(ns ui.splash.page
  (:require [reagent.core :as r]
            [taoensso.timbre :as log]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.utils :as utils :refer [<sub >evt]]))

(defmethod page :route/splash []
  (let [{:keys [google]}                 (<sub [::subs/config])
        {:keys [query]}                  (<sub [::router.subs/active-page])
        {:keys [client-id redirect-uri]} google]
    (r/create-class
      {:display-name        "splash"
       :component-did-mount (fn []
                              (log/debug "component-did-mount" query)
                              (when-let [code (:code query)]
                                (case (-> query :auth keyword)
                                  :google
                                  (>evt [:splash/send-google-verification-code code redirect-uri])
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
