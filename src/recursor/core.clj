(ns recursor.core
  (:require [recursor.util :as u]))

(defonce ^:private
  stack-symbol
  (gensym 'recursor-stack-))

(defn return*
  [v stack]
  (reduce (fn [acc f]
            (f acc))
          v
          stack))

(defmacro return
  "Breaks recursion, returns given value.
  Use inside `recfn`, `letrec`, `defrec`, etc."
  [x]
  `(return* ~x ~stack-symbol))

(defmacro recurse
  "For off-stack recursion.
  Use inside `recfn`, `letrec`, `defrec`, etc."
  [fn-call & {:keys [then]}]
  (let [[_ & args] fn-call]
    `(recur ~@args
            ~(if then
               `(cons ~then
                      ~stack-symbol)
               stack-symbol))))

(defmacro letrec
  "Like `letfn`, but defines `recfn`s instead of `fn`s.

  Note: Use `trampoline` for mutual recursion."
  [fnspecs & body]
  `(letfn [~@(for [[name argv & body] fnspecs
                   :let [duped-bindings (for [arg argv
                                              arg [arg arg]]
                                          arg)]]
               `(~name ~argv
                 (let [~'curr-recursor ~name]
                   (loop [~@duped-bindings
                          ~stack-symbol []]
                     ~@body))))]
     ~@body))

(defmacro recfn
  "Like `fn`, but with a custom call stack.
  - recursive calls to itself should be wrapped in `recurse`
  - base values should be wrapped in `return`"
  [name argv & body]
  `(letrec [(~name ~argv ~@body)]
     ~name))

(defn- defrec*
  [args & [private?]]
  (let [{:keys [name doc argv body]} (u/parse-params args)
        meta-data {:doc doc
                   :arglists [(list 'quote argv)]
                   :private (boolean private?)}
        name-with-meta (u/add-meta name meta-data)]
    `(def ~name-with-meta
       (recfn ~name-with-meta ~argv
          ~@body))))

(defmacro defrec
  "Like `defn`, but defines a `recfn` instead of a `fn`."
  [& args]
  (defrec* args))

(defmacro defrec-
  "Like `defn`, but defines a `recfn` instead of a `fn`."
  [& args]
  (defrec* args true))
