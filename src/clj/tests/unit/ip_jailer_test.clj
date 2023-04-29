(ns tests.unit.ip-jailer-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [api.server :refer [init-state transition jail-time]]))

(deftest jailer []


  (testing "Process IP for the first time"
    (let [ips           (atom {})
          ip            "192.168.1.100"
          now           (System/currentTimeMillis)
          current-state (update (or (@ips ip) (init-state ip)) :timestamps conj now)
          new-state     (transition current-state now)]
      (is (= :free (:state new-state)))))

  (testing "Process same IP four times in a row"
    (let [ips           (atom {})
          ip            "192.168.1.100"
          now           (System/currentTimeMillis)
          current-state {:ip ip :state :free :timestamps [now
                                                          (+ 1 now)
                                                          (+ 2 now)
                                                          (+ 3 now)]}
          new-state     (transition current-state now)]
      (is (= :free (:state new-state)))))

  (testing "Process same IP five times in a row"
    (let [ips           (atom {})
          ip            "192.168.1.100"
          now           (System/currentTimeMillis)
          current-state {:ip ip :state :free :timestamps [now
                                                          (+ 1 now)
                                                          (+ 2 now)
                                                          (+ 3 now)
                                                          (+ 4 now)]}
          new-state     (transition current-state now)]

      (println "@new-state" new-state)


      (is (= :temporarily-jailed (:state new-state)))

      (is (= (+ 4 now jail-time)
             (:jail-lift-time new-state)))

      ))


  )
