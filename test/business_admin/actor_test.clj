(ns business-admin.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [business-admin.actor :as actor]
            [business-admin.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Acme Sole Trader"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :coordinate :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-on-unregistered-client-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "no-such-client" :op :coordinate :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-client")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; payroll processing always escalates (governor invariant)
        request {:client-id "client-1" :op :process-payroll :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "client-1")))))))
