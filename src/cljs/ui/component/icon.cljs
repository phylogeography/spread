(ns ui.component.icon)

(def icons {:spread       "icons/spread.png"
            :dropdown     "icons/icn_dropdown.svg"
            :run-analysis "icons/icn_run_analysis.svg"
            :completed    "icons/icn_previous_analysis.svg"
            :kebab-menu   "icons/icn_kebab menu.svg"
            :delete       "icons/icn_delete.svg"
            :user         "icons/icn_user.svg"
            :queue        "icons/icn_queue.svg"
            :upload       "icons/icn_upload.svg"
            })

(defn icon-with-label []
  (fn [{:keys [icon label on-click]}]
    [:div.icon-label {:on-click on-click}
     [:img {:src icon :id label}]
     [:span.label label]]))
