(defproject clojure-hands-on "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [mount "0.1.11"]
                 [com.github.Ullink/simple-slack-api "e8a9423"]
                 [org.glassfish.tyrus.bundles/tyrus-standalone-client "1.13"]
                 [clj-http "2.3.0"]]
  :repositories {"jitpack" "https://jitpack.io"}
  :main ^:skip-aot clojure-hands-on.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
