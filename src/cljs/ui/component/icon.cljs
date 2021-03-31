(ns ui.component.icon)

(def icons {:spread       "icons/spread.png"
            :dropdown     "icons/icn_dropdown.svg"
            :run-analysis "icons/icn_run_analysis.svg"
            :completed    "icons/icn_previous_analysis.svg"
            :kebab-menu   "icons/icn_kebab menu.svg"
            :delete       "icons/icn_delete.svg"
            :user        "icons/icn_user.svg"
            })

(defn icon-with-label
  []
  (fn [{:keys [icon label]}]
    [:div.icon-label
     [:div.icon
      [:img {:src icon :id label}]]
     [:span.label label]]))
