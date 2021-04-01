(ns ui.component.button)

(defn button-with-icon-and-label [{:keys [icon label on-click class]}]
  [:button {:class    class
            :on-click on-click}
   [:img {:src icon}] label])

(defn button-with-label [{:keys [label on-click class]}]
  [:button {:class    class
            :on-click on-click} label])
