(ns analysis-viewer.events.ui)

(defn toggle-collapsible-tab [db [_ parent-id tab-id]]
  (update-in db [:ui.collapsible-tabs/tabs parent-id tab-id] not))

(defn parameters-select [db [_ param-id value]]
  (assoc-in db [:ui/parameters param-id] value))

