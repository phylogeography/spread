(ns ui.config)

(def default-config
  {:logging

   {:level    :info
    :console? true}

   :router
   {:routes        [["/" :route/home]]
    :default-route :route/home
    :scroll-top?   true
    :html5?        true}

   :graphql
   {:url "http://127.0.0.1:3001/api"}

   })

(def dev-config
  (-> default-config
      (assoc-in [:logging :level] :debug)))

;; TODO: generate based on whether dev, prod, qa
(defn load []
  dev-config)
