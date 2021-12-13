(ns shared.components
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn collapsible-tab [{:keys [id title icon badge-text badge-color child]}]
  (let [open? @(subscribe [:collapsible-tabs/open? id])]
    [:div.tab
     [:div.title {:on-click #(dispatch [:collapsible-tabs/toggle id])}
      [:span.text
       (when icon [:img {:src icon}])
       title]
      (when (and badge-text badge-color)
        [:span.badge {:style {:color badge-color
                              :border-color badge-color}}
         badge-text])
      [:img.arrow {:src "/icons/icn_dropdown.svg" :class (if open? "open" "closed")}]]
     [:div.tab-body {:class (if open? "open" "collapsed")}
      child]]))

(defn spread-logo []
  [:div.spread-logo
   [:img.logo-img {:src "/icons/icn_spread.svg"}]
   [:span.text "spread"]])

(defn button [{:keys [text on-click icon class disabled? opts]}]
  [:div.button.clickable (merge {:class    (cond-> class
                                             disabled? (str " disabled"))
                                 :on-click (if disabled?
                                             identity
                                             on-click)}
                                opts)
   [:img {:src icon}]
   [:span text]])

(defn labeled-field [{:keys [label text style]}]
  [:div.labeled-field {:style style}
   [:span.label label]
   [:span.text text]])

(defn tabs [{:keys [on-change active tabs-vec]}]
  [:div.tabs
   (for [{:keys [id label sub-label]} tabs-vec]
     ^{:key id}
     [:div.tab-wrapper.clickable {:on-click #(on-change nil id)
                                  :class (when (= active id) "active")}
      [:div.tab
       [:div.label label]
       [:div.sub-label sub-label]]])])
