(ns ui.component.icon)

(def icons {:spread "icons/spread.png"
            :dropdown "icons/icn_dropdown.svg"
            :run-analysis "icons/icn_run_analysis.svg"
            :completed "icons/icn_previous_analysis.svg"
            :more "icons/icn_kebab menu.svg"
            })

(defn icon-with-label
  []
  (fn [{:keys [icon label]}]
    [:div.icon-label
     [:div.icon
      [:img {:src icon :id label}]]
     [:span.label label]]))
