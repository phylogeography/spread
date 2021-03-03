(ns spread.events
  (:require [re-frame.core :as re-frame]
            [spread.events.animation :as events.animation]
            [spread.db :as db]
            [ajax.core :as ajax]))

(re-frame/reg-event-fx
 ::initialize
 (fn [_ _]
   {:db db/empty-db
    :fx [[:dispatch [::load-map {:map/name "countries/uruguayLow.json"}]]
         [:dispatch [::load-map {:map/name "countries/argentinaLow.json"}]]
         #_[:dispatch [::load-data {}]]]}))

(re-frame/reg-event-fx
 ::bad-http-result
 (fn [_ ev]
   (js/console.error ev)
   {}))

(re-frame/reg-event-fx
 ::map-loaded
 (fn [{:keys [db]} [_ geo-json-map]]
   (println "MAP LOADED")
   (println geo-json-map)
   {:db (update db :maps conj geo-json-map)}))

(re-frame/reg-event-fx
 ::data-loaded
 (fn [_ ev]
   (js/console.log "DATA LOADED")
   (js/console.log ev)
   {}))

(re-frame/reg-event-fx
 ::load-map
 (fn [_ [_ map-data]]
   {:http-xhrio {:method          :get
                 :uri             (str "http://localhost:8000/maps/" (:map/name map-data))
                 :timeout         8000 
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::map-loaded]
                 :on-failure      [::bad-http-result]}}))

(re-frame/reg-event-fx
 ::load-data
 (fn [_ [_ _]]
   {:http-xhrio {:method          :get
                 :uri             (str "http://localhost:1234/data/")
                 :timeout         8000 
                 :response-format (ajax/json-response-format {:keywords? true}) 
                 :on-success      [::data-loaded]
                 :on-failure      [::bad-http-result]}}))


