(ns tests.unit.redact-test
  (:require [clojure.test :refer [deftest is testing]]
            [shared.utils :refer [redact-secrets]]))

(deftest redact-secrets-test

  (testing "redacts a top-level sensitive value"
    (is (= {:private-key "[REDACTED]" :env "prod"}
           (redact-secrets {:private-key "-----BEGIN RSA PRIVATE KEY-----..."
                            :env         "prod"}
                           #{:private-key}))))

  (testing "redacts sensitive values nested in sub-maps"
    (is (= {:db  {:password "[REDACTED]" :user "spread"}
            :aws {:secret-access-key "[REDACTED]" :region "us-east-2"}}
           (redact-secrets {:db  {:password "CN78MjgZK6B4u3YcW" :user "spread"}
                            :aws {:secret-access-key "wJalrXUtnFEMI" :region "us-east-2"}}
                           #{:password :secret-access-key}))))

  (testing "leaves maps without sensitive keys unchanged"
    (is (= {:env "prod" :api {:port 3001}}
           (redact-secrets {:env "prod" :api {:port 3001}}
                           #{:private-key :password}))))

  (testing "only redacts sensitive keys that are actually present"
    (is (= {:a 1}
           (redact-secrets {:a 1} #{:password})))))
