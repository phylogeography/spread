(ns ui.router.component
  (:require [re-frame.core :refer [subscribe]]
            [ui.router.subs :as subs]))

(defmulti page identity)

(defn router []
  (let [active-page (subscribe [::subs/active-page])]
    (fn []
      (let [{:keys [:name :params :query]} @active-page]
        (when name
          ^{:key (str name params query)} [page name])))))
