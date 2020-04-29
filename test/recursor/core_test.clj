(ns recursor.core-test
  (:require [clojure.test :refer [deftest is]]
            [recursor.core :as r :refer [defrec recurse return]]))

(defrec ^{::r/cache-size 100000}
  A
  "Calculates the ackermann function:
    A(0, n) = n+1
    A(m, 0) = A(m-1, 1)
    A(m, n) = A(m-1, A(m, n-1))"
  [m n]
  (cond
    (zero? m)
    (return (inc n))

    (zero? n)
    (recurse (A (dec m) 1))

    :else
    (recurse
     (A m (dec n))
     :then #(A (dec m) %))))

(deftest recursor-test []
  (is (-> #'A meta :doc (.startsWith "Calculates the ackermann")))
  (is (== 100000 (-> #'A meta ::r/cache-size)))
  (is (== 8189 (A 3 10))))
