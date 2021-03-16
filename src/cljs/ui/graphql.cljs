(ns ui.graphql
  (:require
   ["axios" :as axios]
   [camel-snake-kebab.core :as camel-snake]
   [camel-snake-kebab.extras :as camel-snake-extras]
   [clojure.string :as string]
   [ui.websocket-fx :as websocket]
   [re-frame.core :as re-frame]
   [shared.macros :refer [promise->]]
   [taoensso.timbre :as log]
   [ui.utils :refer [>evt reg-empty-event-fx]]))

(defn gql-name->kw [gql-name]
  (when gql-name
    (let [k (name gql-name)]
      (if (string/starts-with? k "__")
        (keyword k)
        (let [k     (if (string/ends-with? k "_")
                      (str (.slice k 0 -1) "?")
                      k)
              parts (string/split k "_")
              parts (if (< 2 (count parts))
                      [(string/join "." (butlast parts)) (last parts)]
                      parts)]
          (apply keyword (map camel-snake/->kebab-case parts)))))))

(defn gql->clj [m]
  (->> m
       (js->clj)
       (camel-snake-extras/transform-keys gql-name->kw)))

(defmulti handler
  (fn [_ key value]
    (cond
      (:error value) :api/error
      (vector? key)  (first key)
      :else          key)))

(defn- update-db [cofx fx]
  (if-let [db (:db fx)]
    (assoc cofx :db db)
    cofx))

(defn- safe-merge [fx new-fx]
  (reduce (fn [merged-fx [k v]]
            (when (= :db k)
              (assoc merged-fx :db v)))
          fx
          new-fx))

(defn- do-reduce-handlers
  [{:keys [db] :as cofx} f coll]
  (reduce (fn [fxs element]
            (let [updated-cofx (update-db cofx fxs)]
              (if element
                (safe-merge fxs (f updated-cofx element))
                fxs)))
          {:db db}
          coll))

(defn reduce-handlers
  [cofx response]
  (do-reduce-handlers cofx
                      (fn [fxs [k v]]
                        (handler fxs k v))
                      response))

(re-frame/reg-event-fx
  ::response
  (fn [cofx [_ response]]
    (reduce-handlers cofx response)))

(re-frame/reg-fx
  ::query
  (fn [[params callback]]
    (promise-> (axios params)
               callback)))

(re-frame/reg-event-fx
  ::query
  [(re-frame/inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [_ {:keys [query variables]}]]
    (let [url          (get-in db [:config :graphql :url])
          access-token (:access-token localstorage)
          params       (clj->js {:url     url
                                 :method  :post
                                 :headers (merge {"Content-Type" "application/json"
                                                  "Accept"       "application/json"}
                                                 (when access-token
                                                   {"Authorization" (str "Bearer " access-token)}))
                                 :data    (js/JSON.stringify
                                            (clj->js {:query     query
                                                      :variables variables}))})
          callback     (fn [^js response]
                         (if (= 200 (.-status response))
                           ;; TODO we can still have errors even with a 200
                           ;; so we should log them or handle in some other way
                           (>evt [::response (gql->clj (.-data (.-data response)))])
                           (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))]
      {::query [params callback]})))

;; TODO

(reg-empty-event-fx ::ws-authorized)

#_(re-frame/reg-event-fx
  ::ws-authorized
  (fn [{:keys [db ]} [_ resp]]
    (log/debug "ws-authorized" {:resp resp})))

(re-frame/reg-event-fx
  ::ws-authorize
  [(re-frame/inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [_ {:keys [on-timeout]}]]
    (let [url          (get-in db [:config :graphql :ws-url])
          access-token (:access-token localstorage)]

      (log/debug "ws-auth" {:url   url
                            :token access-token
                            :on-timeout on-timeout})

      {:dispatch [::websocket/request :default
                  {:message
                   {:type    "connection_init"
                    :payload {"Authorization"
                              (str "Bearer " access-token)}}
                   :on-response [::ws-authorized]
                   :on-timeout  on-timeout #_[::ws-authorized-failed]
                   :timeout     3000}]})))

(re-frame/reg-event-fx
  ::subscription
  (fn [{:keys [db]} [_ {:keys [id query variables]}]]
    {:dispatch [::websocket/subscribe :default
                id
                {:message
                 {;;:id      id
                  :type    "start"
                  :payload {:variables     variables
                            :extensions    {}
                            :operationName nil
                            :query         query}}
                 :on-message [::subscription-response]
                 ;; :on-close   []
                 }]}))

(re-frame/reg-event-fx
  ::subscription-response
  (fn [cofx [_ response]]
    (log/debug "received subscription response" response)
    (reduce-handlers cofx (gql->clj (get-in response [:payload :data])))))

(defmethod handler :default
  [cofx k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k})
  (reduce-handlers cofx values))

(defmethod handler :discrete-tree-parser-status
  [{:keys [db]} _ {:keys [id status]}]
  (log/debug "discrete-tree-parser-status handler" db)
  {:db (assoc-in db [:discrete-tree-parser id :status] status)})

(defmethod handler :user
  [{:keys [db]} _ {:user/keys [address] :as user}]
  (log/debug "user handler" user)
  {:db (assoc-in db [:users address] user)})

(defmethod handler :google-login
  [_ _ {:keys [access-token]}]
  (log/debug "google-login handler" {:access-token access-token})
  (re-frame/dispatch [:splash/login-success access-token]))


(defmethod handler :api/error
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )
