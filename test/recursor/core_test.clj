(ns recursor.core-test
  (:require [clojure.test :refer [deftest is]]
            [recursor.core :as r :refer [defrec recurse return]]))

(defrec ^{::r/cache-size 5000}
  ackermann
  "Calculates the ackermann function:
    A(0, n) = n+1
    A(m, 0) = A(m-1, 1)
    A(m, n) = A(m-1, A(m, n-1))"
  [m n]
  (cond
    (zero? m)
    (return (inc n))

    (zero? n)
    (recurse (ackermann (dec m) 1))

    :else
    (recurse
     (ackermann m (dec n))
     :then #(ackermann (dec m) %))))

(deftest off-stack-recursion-test []
  (is (.startsWith (-> #'ackermann meta :doc)
                   "Calculates the ackermann"))
  (is (== 5000 (-> #'ackermann meta ::r/cache-size)))
  (is (== 8189 (ackermann 3 10))))
