(ns ui.splash.page
  (:require ["react" :as react]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-header :refer [card-header]]
            [reagent-material-ui.core.divider :refer [divider]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :as styles]
            [reagent.core :as reagent]
            [taoensso.timbre :as log]
            [ui.component.icon :refer [arg->icon icons]]
            [ui.router.component :refer [page]]
            [ui.router.subs :as router.subs]
            [ui.subscriptions :as subs]
            [ui.utils :as utils :refer [<sub >evt]]))

(def use-styles (styles/make-styles (fn [_]
                                      {:root       {:background "#ECEFF8"
                                                    :min-width  "100%"
                                                    :min-height "100vh"}
                                       :card       {:width         "720px"
                                                    :height        "300px"
                                                    :box-shadow    "0px 30px 60px #313B5833"
                                                    :border-radius "20px"
                                                    :opacity       1}
                                       :typography {:width          "134px"
                                                    :height         "35px"
                                                    :text-align     "left"
                                                    :font           "normal normal 900 30px/35px Roboto"
                                                    :letter-spacing "3.9px"
                                                    :color          "#3A3668"
                                                    :text-transform "uppercase"}
                                       :centered   {:display         "flex"
                                                    :flex-direction  "row"
                                                    :justify-content "center"}
                                       :middle     {:position  "absolute"
                                                    :top       "50%"
                                                    :left      "50%"
                                                    :transform "translate(-50%, -50%)"}
                                       :button     {:width          "324px"
                                                    :height         "48px"
                                                    :background     "#F0F0F0 0% 0% no-repeat padding-box"
                                                    :border         "1px solid #F0F0F0"
                                                    :border-radius  "8px"
                                                    :textTransform  "none"
                                                    :font           "normal normal medium 16px/27px Roboto"
                                                    :letter-spacing "0px"
                                                    :color          "#757575"}})))

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
    [grid {:class-name  (:root classes)
           :container   true
           :spacing     0
           :align-items "center"
           :justify     "center"}
     [card {:class-name (:card classes)}
      [:div {:class-name (:centered classes)}
       [card-header {:class-name (:header classes)
                     :avatar     (reagent/as-element [avatar {:alt     "spread"
                                                              :variant "square"
                                                              :src     (arg->icon (:spread icons))}])
                     :title      (reagent/as-element [typography {:class-name (:typography classes)}
                                                      "Spread"])}]]
      [divider {:variant "fullWidth"}]
      [card-content {:class-name (:centered classes)}
       [:div {:class-name (:middle classes)}
        [button {:class-name (:button classes)
                 :variant    "contained"
                 :start-icon (reagent/as-element [:img {:src (arg->icon (:google icons))}])
                 :href       (str "https://accounts.google.com/o/oauth2/v2/auth"
                            "?client_id=" client-id
                            "&scope=email%20profile"
                            "&response_type=code"
                            "&redirect_uri=" (utils/url-encode redirect-uri))}
         "Continue with Google"]]]]]))
