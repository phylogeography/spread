(ns analysis-viewer.events.ui)

(defn toggle-collapsible-tab [db [_ parent-id tab-id]]
  (update-in db [:ui.collapsible-tabs/tabs parent-id tab-id] not)
  )

