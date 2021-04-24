(ns ui.events
  (:require [re-frame.core :as re-frame :refer [reg-event-fx]]
            [ui.events.analysis :as events.analysis]
            [ui.events.discrete-mcc-tree :as events.discrete-mcc-tree]
            [ui.events.continuous-mcc-tree :as events.continuous-mcc-tree]
            [ui.events.general :as events.general]
            [ui.events.graphql :as events.graphql]
            [ui.events.home :as events.home]
            [ui.events.router :as events.router]
            [ui.events.splash :as events.splash]
            [ui.events.websocket :as events.websocket]))

;;;;;;;;;;;;;;;;;;;;
;; General events ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :do-nothing (constantly nil))
(reg-event-fx :log-error (fn [_ ev] (js/console.error ev) {}))

;;;;;;;;;;;;;;;;;;;;
;; Graphql events ;;
;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :graphql/response events.graphql/response)
(reg-event-fx :graphql/query [(re-frame/inject-cofx :localstorage)] events.graphql/query)
(reg-event-fx :graphql/ws-authorized (constantly nil))
(reg-event-fx :graphql/ws-authorize [(re-frame/inject-cofx :localstorage)] events.graphql/ws-authorize)
(reg-event-fx :graphql/ws-authorize-failed events.graphql/ws-authorize-failed)
(reg-event-fx :graphql/subscription-response events.graphql/subscription-response)
(reg-event-fx :graphql/subscription events.graphql/subscription)
(reg-event-fx :graphql/unsubscribe events.graphql/unsubscribe)

;;;;;;;;;;;;;;;;;;;;;;;
;; General UI events ;;
;;;;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :general/active-page-changed events.general/active-page-changed)
(reg-event-fx :general/initialize events.general/initialize)

;;;;;;;;;;;;;;;;;
;; Home events ;;
;;;;;;;;;;;;;;;;;

(reg-event-fx :home/initialize-page events.home/initialize-page)
(reg-event-fx :home/initial-query events.home/initialize-query)

;;;;;;;;;;;;;;;;;;;
;; Splash events ;;
;;;;;;;;;;;;;;;;;;;

(reg-event-fx :splash/initialize-page [(re-frame/inject-cofx :localstorage)] events.splash/initialize-page)
(reg-event-fx :splash/send-google-verification-code events.splash/send-google-verification-code)
(reg-event-fx :splash/login-success [(re-frame/inject-cofx :localstorage)] events.splash/login-success)

;;;;;;;;;;;;;;;;;;;;;;;;;
;; New analysis events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx :continuous-mcc-tree/on-tree-file-selected events.continuous-mcc-tree/on-tree-file-selected)
(re-frame/reg-event-fx :continuous-mcc-tree/tree-file-upload-progress events.continuous-mcc-tree/tree-file-upload-progress)
(re-frame/reg-event-fx :continuous-mcc-tree/tree-file-upload-success events.continuous-mcc-tree/tree-file-upload-success)
(re-frame/reg-event-fx :continuous-mcc-tree/delete-tree-file events.continuous-mcc-tree/delete-tree-file)
(re-frame/reg-event-fx :continuous-mcc-tree/s3-upload events.continuous-mcc-tree/s3-upload)
(re-frame/reg-event-fx :continuous-mcc-tree/start-analysis events.continuous-mcc-tree/start-analysis)
(re-frame/reg-event-fx :continuous-mcc-tree/set-readable-name events.continuous-mcc-tree/set-readable-name)
(re-frame/reg-event-fx :continuous-mcc-tree/set-y-coordinate events.continuous-mcc-tree/set-y-coordinate)
(re-frame/reg-event-fx :continuous-mcc-tree/set-x-coordinate events.continuous-mcc-tree/set-x-coordinate)
(re-frame/reg-event-fx :continuous-mcc-tree/set-most-recent-sampling-date events.continuous-mcc-tree/set-most-recent-sampling-date)
(re-frame/reg-event-fx :continuous-mcc-tree/set-time-scale-multiplier events.continuous-mcc-tree/set-time-scale-multiplier)

;; TODO
(re-frame/reg-event-fx :discrete-mcc-tree/on-tree-file-selected events.discrete-mcc-tree/on-tree-file-selected)
(re-frame/reg-event-fx :discrete-mcc-tree/s3-tree-file-upload events.discrete-mcc-tree/s3-tree-file-upload)
(re-frame/reg-event-fx :discrete-mcc-tree/tree-file-upload-success events.discrete-mcc-tree/tree-file-upload-success)


;;;;;;;;;;;;;;;;;;;;;;;
;; Websockets events ;;
;;;;;;;;;;;;;;;;;;;;;;;

(reg-event-fx :websocket/connect events.websocket/connect)
(reg-event-fx :websocket/disconnect events.websocket/disconnect)
(reg-event-fx :websocket/connected events.websocket/connected)
(reg-event-fx :websocket/disconnected events.websocket/disconnected)
(reg-event-fx :websocket/request events.websocket/request)
(reg-event-fx :websocket/request-response events.websocket/request-response)
(reg-event-fx :websocket/request-timeout events.websocket/request-timeout)
(reg-event-fx :websocket/subscribe events.websocket/subscribe)
(reg-event-fx :websocket/subscription-message events.websocket/subscription-message)
(reg-event-fx :websocket/unsubscribe events.websocket/unsubscribe)
(reg-event-fx :websocket/subscription-closed events.websocket/subscription-closed)
(reg-event-fx :websocket/push events.websocket/push)

;;;;;;;;;;;;;;;;;;;
;; Router events ;;
;;;;;;;;;;;;;;;;;;;

(reg-event-fx :router/start events.router/interceptors events.router/start)
(reg-event-fx :router/active-page-change events.router/interceptors events.router/active-page-change)
(reg-event-fx :router/active-page-changed events.router/interceptors events.router/active-page-changed)
(reg-event-fx :router/watch-active-page events.router/interceptors events.router/watch-active-page)
(reg-event-fx :router/unwatch-active-page events.router/interceptors events.router/unwatch-active-page)
(reg-event-fx :router/navigate events.router/interceptors events.router/navigate)
#_(reg-event-fx :router/replace events.router/interceptors events.router/replace)
(reg-event-fx :router/stop events.router/interceptors events.router/stop)

;;;;;;;;;;;;;;;
;; Analysis  ;;
;;;;;;;;;;;;;;;

(reg-event-fx :analysis/load-continuous-tree-analysis events.analysis/load-continuous-tree-analysis)
