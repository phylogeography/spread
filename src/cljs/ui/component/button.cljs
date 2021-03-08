(ns ui.component.button)

(defn button []
  (fn [{:keys [disabled? active? color size] :as props
        :or   {color     :primary
               active?   true
               disabled? false
               size      :normal}}
       & children]
    (let [props (dissoc props :disabled? :active? :color :size)]
      (into [:a.button
             (merge
               {:class [(case color
                          :primary   "primary"
                          :secondary "secondary")
                        (when disabled? "disabled")
                        (when active? "active")
                        (condp = size
                          :small  "small"
                          :normal ""
                          :large  "large"
                          :auto   "auto")]}
               props)]
            children))))

(defn button-label []
  (fn [props & children]
    (into [:div.button-label props] children)))

#_(defn button-icon-label []
  (fn [{:keys [icon-name label-text inline?]
        :or   {inline? true}}]
    [:div.button-icon-label
     [:div.icon
      [icon {:name icon-name :inline? inline? :color :white}]]
     [:span.label label-text]]))
