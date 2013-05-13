(defproject org.flatland/wakeful "0.5.2"
  :url "https://github.com/flatland/wakeful"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "restful routing alternative"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/java.classpath "0.2.0"]
                 [org.clojure/tools.namespace "0.1.1"]
                 [org.flatland/useful "0.10.1"]
                 [org.flatland/ego "0.2.0"]
                 [ring "1.1.6"]
                 [compojure "1.1.3"]
                 [hiccup "1.0.1"]
                 [clj-json "0.5.0"]]
  :aliases {"testall" ["with-profile" "dev,default:dev,1.5,default" "test"]}
  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}}
  :repositories {"sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}})
