(ns analysis-viewer.events.ui
  (:require [shared.utils :as utils]))

(defn toggle-collapsible-tab [db [_ parent-id tab-id]]
  (let [tab-new-state (not (get-in db [:ui.collapsible-tabs/tabs parent-id tab-id]))
        close-all-tabs (fn [db]
                         (update db :ui.collapsible-tabs/tabs
                                 (fn [sections-map]
                                   (utils/map-map-vals sections-map
                                                       (fn [tabs-map]
                                                         (utils/map-map-vals tabs-map (constantly false)))))))]
    ;; close all the tabs before toggleing so we don't have more than one collapsible tab open at a time
    ;; this is so we don't overflow vertical space. Making it scrollable is tricky, since we can't fix a max-height
    (-> db
        close-all-tabs
        (assoc-in [:ui.collapsible-tabs/tabs parent-id tab-id] tab-new-state))))

(defn toggle-switch-button [db [_ button-id]]
  (update-in db [:ui.switch-buttons/states button-id] not))

(defn parameters-select [db [_ param-id value]]
  (assoc-in db [:ui/parameters param-id] value))

(defn set-timeline-width [db [_ width]]
  (assoc db :analysis/timeline-width width))
