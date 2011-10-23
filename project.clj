(defproject wakeful "0.2.10"
  :description "restful routing alternative"
  :dependencies [[clojure "1.2.0"]
                 [useful "0.7.3"]
                 [clj-json "0.4.2"]
                 [compojure "0.6.5"]
                 [ego "0.1.7"]
                 [hiccup "0.3.5"]
                 [classlojure "0.6.2"]
                 [org.clojure/tools.namespace "0.1.1" :exclusions [org.clojure/java.classpath]]
                 [org.clojars.ninjudd/java.classpath "0.1.2-SNAPSHOT"]]
  :test-dependencies [[ring "0.3.8"]])
