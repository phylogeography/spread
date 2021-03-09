(ns ui.storage
  (:require [re-frame.core :as re-frame]
            [ui.utils :refer [reg-empty-event-fx]]
            [akiroz.re-frame.storage :as localstorage]))

;; both :fx and :cofx keys are optional, they will not be registered if unspecified.
(localstorage/reg-co-fx!
  :spread ;; local storage key
  {:fx   :localstorage ;; re-frame fx ID
   :cofx :localstorage ;; re-frame cofx ID
   })
