(ns ui.splash.page
  (:require [reagent.core :as r]
            [taoensso.timbre :as log]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [reagent-material-ui.styles :as styles]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.container :refer [container]]
            ["react" :as react]
            [ui.utils :as utils :refer [<sub >evt]]))

;; https://github.com/mui-org/material-ui/blob/master/docs/src/pages/getting-started/templates/sign-in/SignIn.js

(def use-styles (styles/make-styles (fn [{:keys [spacing] :as theme}]
                                      {:paper {:margin-top     (spacing 8)
                                               :display       "flex"
                                               :flex-direction "column"
                                               :align-items    "center"}



                                       })))


(defmethod page :route/splash []
  (let [{:keys [google]}                 (<sub [::subs/config])
        {:keys [query]}                  (<sub [::router.subs/active-page])
        {:keys [client-id redirect-uri]} google
        _ (react/useEffect (fn []
                             (log/debug "component-did-mount" query)
                             (when-let [code (:code query)]
                               (case (-> query :auth keyword)
                                 :google
                                 (>evt [:splash/send-google-verification-code code redirect-uri])
                                 nil))))
        classes (use-styles)]

         (prn "@ style classes" classes)


    ))

#_(defmethod page :route/splash []
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
         ;; let [classes (use-styles)]


         ;; (prn "@ style classes" classes)

         [container {:component "main"
                     :max-width "xs"}
          [card
           [card-content

            [typography "Spread"]

            ]


           ]
          ]
         #_[:div
          [:a.button {:href
                      (str "https://accounts.google.com/o/oauth2/v2/auth"
                           "?client_id=" client-id
                           "&scope=email%20profile"
                           "&response_type=code"
                           "&redirect_uri=" (utils/url-encode redirect-uri))}
           "Login with Google"]])})))
