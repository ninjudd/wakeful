(defproject wakeful "0.2.4"
  :description "restful routing alternative"
  :dependencies [[clojure "1.2.0"]
                 [useful "0.7.0-alpha3"]
                 [clj-json "0.4.0"]
                 [compojure "0.6.3"]
                 [ego "0.1.1"]
                 [hiccup "0.3.5"]
                 [org.clojure/tools.namespace "0.1.1" :exclusions [org.clojure/java.classpath]]
                 [org.clojars.ninjudd/java.classpath "0.1.2-SNAPSHOT"]]
  :dev-dependencies [[ring "0.3.8"] ; for some testing
                     [org.clojars.flatland/cake-marginalia "0.6.3"]]
  :tasks [cake-marginalia.tasks])
