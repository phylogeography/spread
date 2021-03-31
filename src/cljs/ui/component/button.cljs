(ns ui.component.button)

(defn button-with-icon-and-label [{:keys [icon label on-click]}]
  [:button {:on-click on-click}
   [:img {:src icon}] label])
