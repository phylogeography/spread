(ns ui.events.router
  (:require [day8.re-frame.async-flow-fx :as async-flow-fx]
            [re-frame.core :refer [trim-v]]
            [ui.router.effects :as effects]
            [ui.router.queries :as queries]))

(def interceptors [trim-v])

(defn start [{:keys [:db]} [{:keys [:bide-router :html5? :scroll-top?]}]]
  {:db (-> db
           (queries/assoc-bide-router bide-router)
           (queries/assoc-html5 html5?)
           (queries/assoc-scroll-top scroll-top?))})

(defn active-page-change [{:keys [:db]} [name params query]]
  (if (queries/bide-router db) ;; Initial :on-navigate is fired before ::start
    {:dispatch [:router/active-page-changed name params query]}
    {::async-flow-fx/async-flow {:first-dispatch [:do-nothing]
                                 :rules          [{:when     :seen?
                                                   :events   [::start]
                                                   :dispatch [:general/active-page-changed name params query]}]}}))

(defn active-page-changed [{:keys [:db]} [name params query]]
  {:db (queries/assoc-active-page db {:name name :params params :query query})})

(defn watch-active-page [_ [watchers]]
  {::effects/watch-active-page watchers})

(defn unwatch-active-page [_ [watchers]]
  {::effects/unwatch-active-page watchers})

(defn navigate [{:keys [:db]} [name params query]]
  (cond-> {::effects/navigate [(queries/bide-router db) name params query]}
    (queries/scroll-top? db) (assoc :window/scroll-to [0 0])))

(defn stop [{:keys [:db]}]
  {:db (queries/dissoc-router db)})
