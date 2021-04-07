(ns ui.component.date-picker
  (:require ["react-datepicker" :default DatePicker]))

(defn date-picker
  "https://reactdatepicker.com"
  [props]
  [:> DatePicker props])
