(defproject recursor "0.2.0"
  :description "Better recursion for Clojure"
  :url "https://github.com/divs1210/recursor"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.cache "1.0.207"]]
  :repl-options {:init-ns recursor.core}
  :profiles {:dev {:plugins [[lein-cloverage "1.1.1"]]}})
