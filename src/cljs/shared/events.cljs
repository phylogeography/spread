(ns shared.events
  (:require [re-frame.core :refer [reg-event-db]]
            [shared.subs]
            [shared.utils :refer [map-map-vals]]))

(reg-event-db
 :collapsible-tabs/toggle
 (fn [db [_ tab-id]]
   (println "Toggling" tab-id)
   (let [tab-new-state (not (get-in db [:ui.collapsible-tabs/tabs tab-id]))
         close-all-tabs (fn [db]
                          (update db :ui.collapsible-tabs/tabs
                                  (fn [tabs]
                                    (map-map-vals tabs (constantly false)))))]
     ;; close all the tabs before toggleing so we don't have more than one collapsible tab open at a time
     ;; this is so we don't overflow vertical space.
     (-> db
         close-all-tabs
         (assoc-in [:ui.collapsible-tabs/tabs tab-id] tab-new-state)))))
