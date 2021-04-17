(ns ui.subscriptions
  (:require [re-frame.core :as re-frame]
            [shared.math-utils :as math-utils]
            [ui.svg-renderer :as svg-renderer]))

(re-frame/reg-sub
  ::config
  (fn [db _]
    (get db :config)))

(re-frame/reg-sub
  ::users
  (fn [db _]
    (-> db :users
        (dissoc :authorized-user))))

(re-frame/reg-sub
  ::authorized-user
  (fn [db _]
    (-> db :users :authorized-user)))

(re-frame/reg-sub
  ::discrete-tree-parsers
  (fn [db _]
    (get db :discrete-tree-parsers)))

(re-frame/reg-sub
  ::discrete-tree-parser
  :<- [::discrete-tree-parsers]
  (fn [discrete-tree-parsers [_ id]]
    (get discrete-tree-parsers id)))

(re-frame/reg-sub
 ::continuous-tree-parsers
 (fn [db _]
   (get db :continuous-tree-parsers)))

(re-frame/reg-sub
 ::continuous-tree-parser
 :<- [::continuous-tree-parsers]
 (fn [continuous-tree-parsers [_ id]]
   (get continuous-tree-parsers id)))

;; TODO : queued parsers

;; TODO : completed parsers

(re-frame/reg-sub
 ::active-continuous-tree-parser
 (fn [db _]
   (let [id (get-in db [:new-analysis :continuous-mcc-tree :continuous-tree-parser-id])]
     (get (get db :continuous-tree-parsers) id))))

(re-frame/reg-sub
  ::continuous-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree])))

(re-frame/reg-sub
  ::continuous-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree :errors])))

;;;;;;;;;;
;; Maps ;;
;;;;;;;;;;

(defn geo-json-data-map [db-maps]
  (let [maps {:type "FeatureCollection"
              :features (->> db-maps
                             (sort-by :map/z-index <)
                             (map :map/geo-json))}]
    (assoc maps :map-box (svg-renderer/geo-json-bounding-box maps))))

(re-frame/reg-sub
 ::map-data
 (fn [db _]
   (let [hide-world? (false? (-> db :map-state :show-world?))]
     (geo-json-data-map (cond->> (:maps db)
                          hide-world? (remove #(zero? (:map/z-index %))))))))

(re-frame/reg-sub
 ::map-view-box
 (fn [{:keys [map-view-box-center map-view-box-radius]} _]
   (math-utils/outscribing-rectangle map-view-box-center map-view-box-radius)))

(re-frame/reg-sub
 ::analysis-data
 (fn [db _]
   (:analysis-data db)))

(re-frame/reg-sub
 ::map-state
 (fn [db _]
   (:map-state db)))

(comment
  @(re-frame/subscribe [::authorized-user])
  @(re-frame/subscribe [::discrete-tree-parsers])
  @(re-frame/subscribe [::discrete-tree-parser "60b08880-03e6-4a3f-a170-29f3c75cb43f"]))
