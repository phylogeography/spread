(ns ui.splash.page
  (:require ["react" :as react]
            [reagent-material-ui.core.avatar :refer [avatar]]
            ;; [reagent-material-ui.core.button :refer [button]]
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
            [shared.components :refer [button spread-logo]]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.component.input :refer [text-input]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.utils :as utils :refer [<sub >evt]]))

(def email-pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defmethod page :route/splash []
  (let [{:keys [google root-url]}               (<sub [::subs/config])
        {{:keys [code token] :as query} :query} (<sub [::router.subs/active-page])
        {:keys [client-id redirect-uri]}        google
        email?                                  (fn [email]
                                                  (and (string? email) (re-matches email-pattern email)))
        email                                   (reagent/atom nil)
        set-email                               #(reset! email %)
        error                                   (reagent/atom nil)
        set-error                               #(reset! error %)
        on-mount                                (fn []
                                                  (log/debug "component-did-mount" query)
                                                  (case (-> query :auth keyword)
                                                    :google
                                                    (when code
                                                      (>evt [:splash/google-login code redirect-uri]))
                                                    :email
                                                    (when token
                                                      (>evt [:splash/email-login token]))
                                                    nil))]
    (fn []
      (on-mount)
      (if (:auth query) ;; if we have :auth key on query means google is redirecting so show a loading-spinner
        [:div.loading-spinner
         [circular-progress {:size 100}]]
        [:div.splash
         [:div.card
          [:div.card-header
           [spread-logo]]
          [:span.sign-in
           "Sign in with email"]
          [:span.sign-in-sub
           "Enter your email address. We will send you a special link that you can sign in with instantly."]
          [text-input {:label       "E-mail address"
                       :opts        {:style {:height "50px"}
                                     :class "email-input"}
                       :error?      @error
                       :helper-text (when @error
                                      "Enter valid email address")
                       :value       @email
                       :on-change   (fn [value]
                                      (if (email? value)
                                        (set-error false)
                                        (set-error true))
                                      (set-email value))}]
          [button {:text      "Send magic link"
                   :class     "send-button"
                   :disabled? (or (nil? @email) @error)
                   :on-click  (fn []
                                (>evt [:splash/send-login-email @email root-url])
                                (set-email nil))}]
          [:h2 {:style {:width         "325px"
                        :text-align    "center"
                        :border-bottom "2px solid #757295"
                        :line-height   "0.1em"
                        :margin        "30px 0 30px"}}
           [:span {:style {:background "#fff"
                           :padding    "0 10px"
                           :color      "#757295"}} "or"]]
          [:a.button.clickable {:class "google-button"
                                :href  (str "https://accounts.google.com/o/oauth2/v2/auth"
                                            "?client_id=" client-id
                                            "&scope=email%20profile"
                                            "&response_type=code"
                                            "&redirect_uri=" (utils/url-encode redirect-uri))}

           [:img {:src (arg->icon (:google icons))}]
           [:span "Continue with Google"]]]]))))
