(ns kotodama.organism-test
  "The generic organism runtime drives ANY actor via injected :validate/:emit —
  verified here with a domain-free mock actor (no UNSPSC, no taxonomy)."
  (:require [clojure.test :refer [deftest is testing]]
            [kotodama.organism :as org]
            [kotodama.life :as life]))

(def ^:private taxon {:code "T1" :title "Test Actor"})

;; a trivial injected capability: ok iff the input is non-empty
(defn- mock-validate [_taxon input]
  (if (seq input)
    {:ok true  :missing []    :checks [] :domain :test}
    {:ok false :missing ["x"] :checks [] :domain :test}))

(deftest generic-actor-runs-injected-validate
  (let [a (org/actor {:taxon taxon :validate mock-validate})]
    (testing "empty input → not ok, with missing"
      (let [r (org/run a {})]
        (is (false? (:ok r)))
        (is (seq (:missing r)))
        (is (= "T1" (:code r)))
        (is (string? (:reasoning r)))))
    (testing "complete input → ok"
      (is (true? (:ok (org/run a {:a 1})))))))

(deftest prior-consensus-shortcut-is-generic
  (let [a (org/actor {:taxon taxon :validate mock-validate})
        strong (life/prior-consensus
                (repeat 3 {:input {:a 1} :result {:status "authorized"}}) {:a 1})]
    (testing "a shortcut-firing prior-consensus skips validate (ok + shortcut)"
      (let [r (org/run a {:prior-consensus strong :a 1})]
        (is (true? (:ok r)))
        (is (true? (:shortcut r)))))
    (testing "no prior-consensus → normal path"
      (is (nil? (:shortcut (org/run a {})))))))

(deftest validate-is-required
  (is (thrown? clojure.lang.ExceptionInfo (org/actor {:taxon taxon}))))

(deftest emit-can-be-overridden
  (let [a (org/actor {:taxon taxon :validate mock-validate
                      :emit (fn [t v _] {:custom true :code (:code t) :ok (:ok v)})})
        r (org/run a {:a 1})]
    (is (true? (:custom r)))
    (is (= "T1" (:code r)))))
