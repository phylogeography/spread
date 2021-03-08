(ns ui.component.icon)

#_(defn icon []
  (fn [{:keys [name color size inline?]
        :or {name :about
             color :primary
             size :normal
             inline? true}
        :as props}]
    (let [props (dissoc props :name :color :size :inline?)

          color-class (case color
                        :primary "primary"
                        :secondary "secondary"
                        :white "white"
                        :black "black"
                        :dark-blue "dark-blue"
                        :none "")

          [width height] (case size
                           :x-small [8 8]
                           :smaller [12 12]
                           :small [16 16]
                           :normal [24 24]
                           :large [32 32])

          src (icon-src name color)
          style (-> icon-listing name :style)]

      (assert src (str "Given icon does not exist. Name: " name " Color: " color))
      [:div.icon (merge props {:style (or style {})})
       (if-not inline?
         [:img {:src src
                :style {:width (str width "px")
                        :height (str height "px")}}]
         [c-inline-svg {:src src
                        :width width
                        :height height
                        :class (str "icon-svg " color-class)}])])))
