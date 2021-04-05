(ns ui.component.date-picker
  (:require ["react-datepicker" :default DatePicker]
            [reagent.core :as reagent]))

(defn date-picker
  "https://reactdatepicker.com"
  [{:keys [:selected :on-change] :as props}]
  [:> DatePicker props])
