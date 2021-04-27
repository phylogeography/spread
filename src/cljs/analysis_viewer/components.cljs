(ns analysis-viewer.components
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [goog.string :as gstr]))

(defn switch-button [{:keys [id]}]
  (let [on? @(subscribe [:switch-buttons/on? id])]
    [:div.switch-button {:class (if on? "on" "off")
                         :on-click (fn [evt]
                                     (.stopPropagation evt)
                                     (dispatch [:switch-button/toggle id]))}
     [:span.on "On"]
     [:span.block {:class (if on? "block-on" "block-off")}]
     [:span.off "Off"]]))

(defn slider [{:keys [inc-buttons length vertical? subs-vec ev-vec min-val max-val  class]}]
  (let [state (r/atom {:grab-screen-val nil
                       :grab-screen-length nil}) ;; if it is vertical? it will store y, x otherwise
        val->screen-length (fn [v] (/ (* (- v min-val) length) (- max-val min-val)))
        screen-length->val (fn [sl]
                             (let [v (/ (* sl (- max-val min-val)) length)]
                               (+ min-val v)))
        gap 5
        start 10]
    (fn []
      (let [{:keys [grab-screen-val grab-screen-length]} @state
            val @(subscribe subs-vec)
            screen-length (val->screen-length val)
            line-props (if vertical?
                         {:x1 start :y1 (- length gap) :x2 start :y2 gap :stroke-width 3
                          :stroke-dashoffset (- length screen-length)}
                         {:x1 (- length gap) :y1 start :x2 gap :y2 start :stroke-width 3
                          :stroke-dashoffset (+ length screen-length)})
            buttons-extra-size (if inc-buttons 40 0)
            slider-width  (str (if vertical? 20 (+ length buttons-extra-size)) "px")
            slider-height (str (if vertical? (+ length buttons-extra-size) 20) "px")
            block-x (if vertical? 4 (- screen-length start))
            block-y (if vertical? (- length screen-length 6) 4)
            set-new-val (fn [new-val]
                          (when (<= min-val new-val max-val)
                            (dispatch (conj ev-vec new-val))))
            inc-btn [:button.plus {:on-click #(set-new-val (+ val inc-buttons))} "+"]
            dec-btn [:button.plus {:on-click #(set-new-val (- val inc-buttons))} "-"]]
       [:div.slider {:class class                     
                     :on-mouse-move (fn [evt]
                                      (when grab-screen-val
                                        (let [new-coord-val (if vertical?
                                                              (-> evt .-nativeEvent .-offsetY)
                                                              (-> evt .-nativeEvent .-offsetX))
                                              screen-change (- new-coord-val grab-screen-val)
                                              new-screen-length (if vertical?
                                                                  (- grab-screen-length screen-change)
                                                                  (+ grab-screen-length screen-change))]
                                          (set-new-val (screen-length->val new-screen-length)))))
                     :on-mouse-up (fn [evt]                                 
                                    (swap! state dissoc :grab-screen-val :grab-screen-length))}        
        [:div.outer {:class (if vertical? "vertical" "horizontal")
                     :style {:width slider-width :height slider-height}}
         (when inc-buttons (if vertical? inc-btn dec-btn))
         [:svg {:width slider-width :height slider-height}
          [:line (assoc line-props :stroke "#DEDEE8")]
          [:line (assoc line-props
                        :stroke "#EEBE53"
                        :stroke-dasharray length)]
          [:rect {:x block-x
                  :y block-y
                  :width 12 :height 12 :fill "white" :stroke "grey"
                  :on-mouse-down (fn [evt]
                                   (let [coord (if vertical?
                                                 (-> evt .-nativeEvent .-offsetY)
                                                 (-> evt .-nativeEvent .-offsetX))]                                     
                                     (swap! state assoc :grab-screen-val coord :grab-screen-length screen-length)))}]]
         (when inc-buttons (if vertical? dec-btn inc-btn))]]))))
