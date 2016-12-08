(defproject ul-feedback "0.1.0-SNAPSHOT"
  :description "This is a not so toy teaching clojure by writing a complete feedback system attached to a slack bot."
  :url "https://github.com/Ullink/ul-feedback"
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot ul-feedback.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
