(ns recursor.core-test
  (:require [clojure.test :refer [deftest is]]
            [recursor.core :refer [defrec letrec recurse return]]))

(let [known (volatile! {})
      save! (fn [k v]
              (vswap! known assoc k v)
              v)]
  (defrec ackermann [m n]
    (cond
      (@known [m n])
      (return (@known [m n]))

      (zero? m)
      (return (inc n))

      (zero? n)
      (recurse
       (ackermann (dec m) 1)
       :then #(save! [(dec m) 1] %))

      :else
      (recurse
       (ackermann m (dec n))
       :then (fn [n']
               (save! [m (dec n)] n')
               (ackermann (dec m) n'))))))

(deftest off-stack-recursion-test []
  (is (== 8189 (ackermann 3 10))))


(letrec [(is-odd? [n]
           #(if (zero? n)
              (return false)
              (is-even? (dec n))))
         (is-even? [n]
           #(if (zero? n)
              (return true)
              (is-odd? (dec n))))]
  (deftest mutual-recursion-test
    (is (trampoline is-even? 10000))))
