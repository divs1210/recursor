(ns recursor.core
  (:require [clojure.core.cache.wrapped :as cw]
            [recursor.util :as u]))

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
  Use inside `recfn`, `letrec`, `defrec`, etc.
  To perform a calculation with the returned value of
  this recursive call, pass a `fn` as `:then`.
  To perform multiple calculations, pass a list of
  `fn`s as `:thens` in the required order."
  [fn-call & {:keys [then thens]}]
  (let [[op & args] fn-call
        cache-name (u/cache-name op)
        env (or &env {})
        cached? (env cache-name)
        save! (when cached?
                `(fn [v#]
                   (cw/lookup-or-miss ~cache-name
                                      [~@args]
                                      (fn [_#] v#))))
        thens+ (vec (remove nil? (cons save! (cons then thens))))
        new-stack (if (seq thens+)
                    `(concat ~thens+
                             ~stack-symbol)
                    stack-symbol)]
    `(recur ~@args ~new-stack)))

(defmacro letrec
  "Like `letfn`, but defines `recfn`s instead of `fn`s.

  **Note:** `recfn`s are not mutually recrsive yet.
  Use `trampoline` for mutual recursion."
  [fnspecs & body]
  `(let [~@(for [[name] fnspecs
                 :let [cache-name (u/cache-name name)
                       cache-size (some->> name
                                           meta
                                           ::cache-size)]
                 :when cache-size
                 e [cache-name
                    `(cw/lu-cache-factory
                      {} :threshold ~cache-size)]]
             e)]
     (letfn [~@(for [[name argv & body] fnspecs
                     :let [internal-name (u/internal-name name)
                           cache-name (u/cache-name name)
                           duped-bindings (for [arg argv
                                                arg [arg arg]]
                                            arg)
                           cached? (some->> name meta ::cache-size)]
                     a-recfn [`(~internal-name ~argv
                                (loop [~@duped-bindings
                                       ~stack-symbol []]
                                  ~(if cached?
                                     `(if (cw/has? ~cache-name ~argv)
                                        (return (cw/lookup ~cache-name ~argv))
                                        (let []
                                          ~@body))
                                     `(let []
                                        ~@body))))
                              `(~name ~argv
                                ~(if cached?
                                   `(cw/lookup-or-miss ~cache-name
                                                       ~argv
                                                       (fn [k#]
                                                         (apply ~internal-name k#)))
                                   `(~internal-name ~@argv)))]]
                 a-recfn)]
       ~@body)))

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
