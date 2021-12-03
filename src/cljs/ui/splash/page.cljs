(ns ui.splash.page
  (:require ["react" :as react]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-header :refer [card-header]]
            [reagent-material-ui.core.circular-progress :refer [circular-progress]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [taoensso.timbre :as log]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.component.input :refer [amount-input loaded-input text-input]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.utils :as utils :refer [<sub >evt]]))

(def use-styles (styles/make-styles (fn [_]
                                      {:grid {:background "#ECEFF8"
                                              :min-width  "100%"
                                              :min-height "100vh"}

                                       :card {:min-width     "720px"
                                              :box-shadow    "0px 30px 60px #313B5833"
                                              :border-radius "20px"}

                                       :card-header {:margin-left "35%"}

                                       :card-header-title {:text-align     "left"
                                                           :font           "normal normal 900 30px/35px Roboto"
                                                           :letter-spacing "3.9px"
                                                           :color          "#3A3668"
                                                           :text-transform "uppercase"}

                                       :card-content {:display         "flex"
                                                      :flex-direction  "column"
                                                      :align-items     :center
                                                      :justify-content :space-evenly}

                                       :sign-in {:text-align :center
                                                 :font       "normal normal 900 24px/28px Roboto"
                                                 :color      "#3A3668"
                                                 :margin     "10px"
                                                 :padding    "10px"}

                                       :sign-in-sub {:width      "440px"
                                                     :text-align :left
                                                     :font       "normal normal medium 16px/19px Roboto"
                                                     :color      "#757295"
                                                     :margin     "10px"
                                                     :padding    "10px"}

                                       :send-button {:width          "325px"
                                                     :height         "50px"
                                                     :text-align     :center
                                                     :font           "normal normal medium 16px/19px Roboto"
                                                     :text-transform :none
                                                     :color          "white"
                                                     :background     "#3428CA"}

                                       :google-button {:width          "325px"
                                                       :height         "50px"
                                                       :background     "#F0F0F0 0% 0% no-repeat padding-box"
                                                       :border         "1px solid #F0F0F0"
                                                       :border-radius  "8px"
                                                       :text-transform :none
                                                       :font           "normal normal medium 16px/27px Roboto"
                                                       :color          "#757575"}

                                       })))

(defmethod page :route/splash []
  (let [{:keys [google]}                 (<sub [::subs/config])
        {:keys [query]}                  (<sub [::router.subs/active-page])
        {:keys [client-id redirect-uri]} google
        _                                (react/useEffect (fn []
                                                            (log/debug "component-did-mount" query)
                                                            (when-let [code (:code query)]
                                                              (case (-> query :auth keyword)
                                                                :google
                                                                (>evt [:splash/send-google-verification-code code redirect-uri])
                                                                nil))))
        classes                          (use-styles)]
    (if (:auth query) ;; if we have :auth key on query means google is redirecting so show a loading-spinner
      [:div.loading-spinner
       [circular-progress {:size 100}]]

      [grid {:class-name  (:grid classes)
             :container   true
             :spacing     0
             :align-items "center"
             :justify     "center"}

       [card {:class-name (:card classes)}

        [card-header {:class-name (:card-header classes)
                      :avatar     (reagent/as-element [avatar {:alt     "spread"
                                                               :variant "square"
                                                               :src     (arg->icon (:spread icons))}])
                      :title      (reagent/as-element [typography {:class-name (:card-header-title classes)}
                                                       "Spread"])}]

        [divider {:variant "fullWidth"}]

        [card-content {:class-name (:card-content classes)}

         [typography {:class-name (:sign-in classes)} "Sign in with email"]

         [typography {:class-name (:sign-in-sub classes)} "Enter your email address. We will send you a special link that you can sign in with instantly."]

         [text-input {;;:class-name (:email-input classes)
                      :label       "E-mail address"
                      :opts        {:style {:width   "325px"
                                            :margin  "10px"
                                            :padding "10px"
                                            ;; :border        "1px solid #DD0808"
                                            ;; :border-radius "10px"
                                            ;; :height        "50px"
                                            }}
                      :error?      true
                      :helper-text "Enter valid email address"
                      ;; :value :todo
                      ;; :on-change (fn [value] )
                      }]

         [button {:class-name (:send-button classes)
                  :variant    "contained"
                  :on-click   (fn [] (prn "TODO" ))}
          "Send magic link"]

         [:h2 {:style {:width         "325px"
                       :text-align    "center" ;
                       :border-bottom "2px solid #757295"
                       :line-height   "0.1em"
                       :margin        "30px 0 30px"}}
          [:span {:style {:background "#fff"
                          :padding    "0 10px"
                          :color      "#757295"}} "or"]]

         [button {:class-name (:google-button classes)
                  :variant    "contained"
                  :start-icon (reagent/as-element [:img {:src (arg->icon (:google icons))}])
                  :href       (str "https://accounts.google.com/o/oauth2/v2/auth"
                                   "?client_id=" client-id
                                   "&scope=email%20profile"
                                   "&response_type=code"
                                   "&redirect_uri=" (utils/url-encode redirect-uri))}
          "Continue with Google"]]]])))
