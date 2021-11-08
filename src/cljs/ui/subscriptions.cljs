(ns ui.subscriptions
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]))

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
  ::search
  (fn [db]
    (:search db)))

(re-frame/reg-sub
  ::analysis
  (fn [db]
    (-> db :analysis)))

(re-frame/reg-sub
  ::sorted-analysis
  :<- [::analysis]
  (fn [analysis]
    (sort-by :created-on (-> analysis vals vec))))

(re-frame/reg-sub
  ::analysis-results
  :<- [::analysis]
  (fn [analysis [_ id]]
    (get analysis id)))

(re-frame/reg-sub
  ::ongoing-analysis
  :<- [::sorted-analysis]
  (fn [analysis]
    (filter (fn [elem]
              (#{"UPLOADED" "ATTRIBUTES_PARSED" "ARGUMENTS_SET"} (:status elem)))
            analysis)))

(re-frame/reg-sub
  ::completed-analysis
  :<- [::sorted-analysis]
  (fn [analysis]
    (filter (fn [elem]
              (#{"ERROR" "SUCCEEDED"} (:status elem)))
            analysis)))

(re-frame/reg-sub
  ::completed-analysis-search
  :<- [::completed-analysis]
  :<- [::search]
  (fn [[completed-analysis search-term]]
    (if search-term
      (filter (fn [elem]
                (let [readable-name (-> elem :readable-name)]
                  (when readable-name
                    (string/includes? (string/lower-case readable-name) (string/lower-case search-term)))))
              completed-analysis)
      completed-analysis)))

(re-frame/reg-sub
  ::new-completed-analysis
  :<- [::completed-analysis]
  (fn [[completed-analysis]]
    (filter (fn [elem]
              (:new? elem))
            completed-analysis)))

(re-frame/reg-sub
  ::queued-analysis
  :<- [::sorted-analysis]
  (fn [analysis]
    (filter (fn [elem]
              (#{"QUEUED""RUNNING"} (:status elem)))
            analysis)))

(re-frame/reg-sub
  ::continuous-mcc-tree
  (fn [db]
    (let [ongoing-analysis (get-in db [:new-analysis :continuous-mcc-tree])
          id               (:id ongoing-analysis)]
      (merge ongoing-analysis
             (get-in db [:analysis id])))))

(re-frame/reg-sub
  ::discrete-mcc-tree
  (fn [db]
    (let [ongoing-analysis (get-in db [:new-analysis :discrete-mcc-tree])
          id               (:id ongoing-analysis)]
      (merge ongoing-analysis
             (get-in db [:analysis id])))))

(re-frame/reg-sub
  ::bayes-factor
  (fn [db]
    (let [ongoing-analysis (get-in db [:new-analysis :bayes-factor])
          id               (:id ongoing-analysis)]
      (merge ongoing-analysis
             (get-in db [:analysis id])))))

(comment
  @(re-frame/subscribe [::authorized-user])
  @(re-frame/subscribe [::discrete-tree-parsers])
  @(re-frame/subscribe [::discrete-tree-parser "60b08880-03e6-4a3f-a170-29f3c75cb43f"]))
