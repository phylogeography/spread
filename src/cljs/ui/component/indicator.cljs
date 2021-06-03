(ns ui.component.indicator)

(defn busy []
  [:div.busy])

(defn loading []
  [:div.loading
   [:h4 "loading.."]])
