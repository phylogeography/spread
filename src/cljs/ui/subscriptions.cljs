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

#_(re-frame/reg-sub
    ::discrete-tree-parsers
    (fn [db _]
      (get db :discrete-tree-parsers)))

#_(re-frame/reg-sub
    ::discrete-tree-parser
    :<- [::discrete-tree-parsers]
    (fn [discrete-tree-parsers [_ id]]
      (get discrete-tree-parsers id)))

#_(re-frame/reg-sub
    ::continuous-tree-parsers
    (fn [db _]
      (get db :continuous-tree-parsers)))

#_(re-frame/reg-sub
    ::continuous-tree-parser
    :<- [::continuous-tree-parsers]
    (fn [continuous-tree-parsers [_ id]]
      (get continuous-tree-parsers id)))

;; TODO
(re-frame/reg-sub
  ::parsers
  (fn [db]
    (-> db :parsers vals)))

(re-frame/reg-sub
  ::queued-analysis
  :<- [::parsers]
  (fn [parsers]
    (reverse
      (filter #(#{"QUEUED" "RUNNING" "SUCCEEDED" "ERROR"} (:status %))
              parsers))))

(re-frame/reg-sub
  ::user-analysis
  (fn [db]
    (-> db :user-analysis :analysis)))

(re-frame/reg-sub
  ::completed-analysis
  :<- [::user-analysis]
  (fn [analysis]
    (filter (fn [elem]
              (#{"ERROR" "SUCCEEDED"} (:status elem)))
            analysis)))

(re-frame/reg-sub
  ::search-term
  (fn [db]
    (-> db :user-analysis :search-term)))

(re-frame/reg-sub
  ::completed-analysis-search
  :<- [::completed-analysis]
  :<- [::search-term]
  (fn [[completed-analysis search-term]]
    (if search-term
      (filter (fn [elem]
                (let [readable-name (-> elem :readable-name)]
                  (when readable-name
                    (string/includes? (string/lower-case readable-name) search-term))))
              completed-analysis)
      completed-analysis)))

(re-frame/reg-sub
  ::continuous-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree])))

(re-frame/reg-sub
  ::continuous-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :continuous-mcc-tree :errors])))

(re-frame/reg-sub
  ::discrete-mcc-tree
  (fn [db]
    (get-in db [:new-analysis :discrete-mcc-tree])))

(re-frame/reg-sub
  ::discrete-mcc-tree-field-errors
  (fn [db]
    (get-in db [:new-analysis :discrete-mcc-tree :errors])))

(re-frame/reg-sub
  ::bayes-factor
  (fn [db]
    (get-in db [:new-analysis :bayes-factor])))

(re-frame/reg-sub
  ::bayes-factor-field-errors
  (fn [db]
    (get-in db [:new-analysis :bayes-factor :errors])))

(comment
  @(re-frame/subscribe [::authorized-user])
  @(re-frame/subscribe [::discrete-tree-parsers])
  @(re-frame/subscribe [::discrete-tree-parser "60b08880-03e6-4a3f-a170-29f3c75cb43f"]))
