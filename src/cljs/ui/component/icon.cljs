(ns ui.component.icon)

(def icons {:spread "icons/spread.png"})

(defn icon-with-label
  []
  (fn [{:keys [icon label]}]
    [:div.icon-label
     [:div.icon
      [:img {:src icon :id label}]]
     [:span.label label]]))
