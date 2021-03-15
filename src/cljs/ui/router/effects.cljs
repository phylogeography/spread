(ns ui.router.effects
  (:require [bide.core :as bide]
            [re-frame.core :as re :refer [dispatch reg-fx]]))

(reg-fx
  ::navigate
  (fn [[& args]]
    (apply bide/navigate! args)))

(reg-fx
  ::replace
  (fn [[& args]]
    (apply bide/replace! args)))

(defn- post-event-callback-fn [opts]
  (fn [event-v]
    (let [watched-name                   (:name opts)
          watched-params                 (:params opts)
          watched-query                  (:query opts)
          [event-name name params query] event-v]
      ;; NOTE: not very idiomatic, can we refactor and use a referred namespace keyword?
      (when (and (= :ui.router.events/active-page-changed event-name)
                 (or (nil? watched-name)
                     (and (or (keyword? watched-name)
                              (string? watched-name))
                          (= watched-name name))
                     (and (sequential? watched-name)
                          (contains? (set watched-name) name))
                     (and (fn? watched-name)
                          (watched-name name)))
                 (or (nil? watched-params)
                     (and (map? watched-params)
                          (= watched-params params))
                     (and (fn? watched-params)
                          (watched-params params)))
                 (or (nil? watched-query)
                     (and (map? watched-query)
                          (= watched-query query))
                     (and (fn? watched-query)
                          (watched-query query))))
        (dispatch (conj (vec (:dispatch opts)) name params query))
        (when (seq (:dispatch-n opts))
          (doseq [dispatch-v (remove nil? (:dispatch-n opts))]
            (dispatch (conj (vec dispatch-v) name params query))))))))

(reg-fx
  ::watch-active-page
  (fn [watchers]
    (let [watchers (if (sequential? watchers) watchers [watchers])]
      (doseq [{:keys [:id] :as watch-opts} watchers]
        (re/add-post-event-callback id (post-event-callback-fn watch-opts))))))

(reg-fx
  ::unwatch-active-page
  (fn [watchers]
    (let [watchers (if (sequential? watchers) watchers [watchers])]
      (doseq [{:keys [:id]} watchers]
        (re/remove-post-event-callback id)))))

#_(reg-fx
  :window/scroll-to
  (fn [[x y]]
    (.scrollTo js/window x y)))
