# recursor
[![Clojars Project](https://img.shields.io/clojars/v/recursor.svg)](https://clojars.org/recursor)
[![CircleCI](https://circleci.com/gh/divs1210/recursor/tree/master.svg?style=svg)](https://circleci.com/gh/divs1210/recursor/tree/master)
[![codecov](https://codecov.io/gh/divs1210/recursor/branch/master/graph/badge.svg)](https://codecov.io/gh/divs1210/recursor)

Better recursion for Clojure.

## Usage

Here's the Ackermann function:

```
A(0, n) = n+1
A(m, 0) = A(m-1, 1)
A(m, n) = A(m-1, A(m, n-1))
```

Here's a straightforward implementation in Clojure:

```clojure
(defn A [m n]
  (cond
    (zero? m) (inc n)
    (zero? n) (recur (dec m) 1)
    :else (recur (dec m) (A m (dec n)))))
```

Great! Let's try it out!

```clojure
user> (time (A 3 8))
"Elapsed time: 85.518103 msecs"
;; => 2045
user> (time (A 4 1))
;; StackOverflowError !!!
```

Now, while you:

1. look for a mathematician, and
2. plead them to derive an iterative version of the function, and
3. refuse to budge till they optimize it

I will do this:

```clojure
(require ' [recursor.core :as r :refer [defrec recurse return]])

(defrec ^{::r/cache-size 100000}
  A
  [m n]
  (cond
    (zero? m) (return (inc n))
    (zero? n) (recurse (A (dec m) 1))
    :else (recurse (A m (dec n))
            :then #(A (dec m) %))))
```

Let's see what it can do:

```clojure
user> (time (A 3 8))
"Elapsed time: 95.970201 msecs"
;; => 2045
user> (time (A 4 1))
"Elapsed time: 2790.475629 msecs"
;; => 65533
```

It achieves this by:

1. Using a custom **unbounded** stack
2. **Optionally** using an [LU cache](https://github.com/clojure/core.cache/wiki/LU) to remember past results if specified in the metadata

It also provides replacements for some other Clojure core forms, all of which can be memoized by setting `^{::r/cache-size <SOME-NUMBER>}` before their names:

| clojure.core  | recursor.core |
|:-------------:|:-------------:|
| `fn`          | `recfn`       |
| `defn`        | `defrec`      |
| `defn-`       | `defrec-`     |
| `letfn`       | `letrec`      |

All of these work in the way you would expect them to, with the following caveats:

1. No varargs in `recfn`s - use a `fn` with varargs to dispatch to fixed-arity `recfn`
2. No multiple-arity `recfn`s - use a multi-arity `fn` to dispatch to fixed-arity `recfn`
3. No destructuring maps in arguments vector - `(recfn f [{:keys [a b]}])`
4. No mutual recursion support - use [trampoline](https://clojuredocs.org/clojure.core/trampoline) as usual

All this is subject to change **without** breaking the API.

## License

Copyright © 2020 Divyansh Prakash

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
