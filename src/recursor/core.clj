(ns recursor.core
  (:require [recursor.util :as u]
            [clojure.core.memoize :as memo]))

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
  (if (u/curr-recursor &env)
    `(return* ~x ~stack-symbol)
    (throw (Exception. "return should be used inside a recfn!"))))

(defmacro recurse
  "For off-stack recursion.
  Use inside `recfn`, `letrec`, `defrec`, etc."
  [fn-call & {:keys [then]}]
  (let [[op & args] (if (seq? fn-call)
                      fn-call
                      nil)
        recursor (u/curr-recursor &env)]
    (cond
      (nil? recursor)
      (throw (Exception. "recurse should be used inside a recfn!"))

      (not= op recursor)
      (throw (Exception. (str "Expected fn-call to be like ("
                              (u/curr-recursor &env)
                              " ...), but got " fn-call)))

      :else
      `(recur ~@args
              ~(if then
                 `(cons ~then
                        ~stack-symbol)
                 stack-symbol)))))

(defmacro letrec
  "Like `letfn`, but defines `recfn`s instead of `fn`s.

  Note: Does NOT support mutual recursion yet."
  [fnspecs & body]
  `(letfn [~@(for [[name argv & body] fnspecs
                   :let [duped-bindings (for [arg argv
                                              arg [arg arg]]
                                          arg)]]
               (if (some #(and (symbol? %)
                               (= #'return (resolve %)))
                         (flatten body))
                 `(~name ~argv
                   (let [~'curr-recursor ~name]
                     (loop [~@duped-bindings
                            ~stack-symbol []]
                       ~@body)))
                 (throw (Exception. "recfns should have at least one call to return!"))))]
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
