(ns ui.graphql
  (:require [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.string :as string]
            [shared.macros :refer [promise->]]
            [ui.utils :refer [>evt]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            ["axios" :as axios]))

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
       (camel-snake-extras/transform-keys gql-name->kw )))

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

;; TODO : put token in store
(re-frame/reg-event-fx
  ::query
  (fn [{:keys [db]} [_ {:keys [query variables]}]]
    (let [url          (get-in db [:config :graphql :url])
          access-token (get-in db [:tokens :access-token])
          _ (log/debug "@@@ QUERY" {:url url
                                    :config (get-in db [:config])})
          params       (clj->js {:url     url
                                 :method  :post
                                 :headers (merge {"Content-Type" "application/json"
                                                  "Accept"       "application/json"}
                                                 (when access-token
                                                   {"access_token" access-token}))
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

(defmethod handler :default
  [cofx k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k})
  (reduce-handlers cofx values))

(defmethod handler :user
  [{:keys [db]} _ {:user/keys [address] :as user}]
  (log/debug "user handler" user)
  {:db (assoc-in db [:users address] user)})

(defmethod handler :google-login
  [{:keys [db]} _ {:user/keys [address google-username] :as user}]
  (log/debug "google-login handler" user)
  {:db (assoc-in db [:users address] (merge user
                                            {:user/user-name google-username}))})

(defmethod handler :api/error
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )
