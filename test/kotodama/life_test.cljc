(ns kotodama.life-test
  "Stage-D prior-consensus + joucho fold — pure, generic."
  (:require [clojure.test :refer [deftest is testing]]
            [kotodama.life :as life]))

(deftest prior-consensus-parity
  (testing "Oracle A: mixed-status priors, one matching current input"
    (is (= {:outcome-count 3 :dominant-status "authorized" :dominant-count 2
            :confidence-permille 666 :input-match-count 2}
           (life/prior-consensus
            [{:input {:quantity 5} :result {:status "authorized"}}
             {:input {:quantity 5} :result {:status "authorized"}}
             {:input {:quantity 9} :result {:status "rejected"}}]
            {:quantity 5}))))
  (testing "Oracle B: empty"
    (is (= {:outcome-count 0 :dominant-status nil :dominant-count 0
            :confidence-permille 0 :input-match-count 0}
           (life/prior-consensus [] {})))))

(deftest prior-shortcut-threshold
  (is (false? (life/prior-shortcut?
               (life/prior-consensus
                [{:input {:quantity 5} :result {:status "authorized"}}
                 {:input {:quantity 5} :result {:status "authorized"}}
                 {:input {:quantity 9} :result {:status "rejected"}}]
                {:quantity 5}))))
  (is (true? (life/prior-shortcut?
              (life/prior-consensus
               (repeat 3 {:input {:quantity 5} :result {:status "authorized"}})
               {:quantity 5})))))

(deftest joucho-folds-events
  (is (= :stressed (life/mood-label (life/joucho-from-events (repeat 6 {:kind :reject})))))
  (is (empty? (life/unknown-event-kinds [{:kind :merge} {:kind :invoke-ok}]))))
