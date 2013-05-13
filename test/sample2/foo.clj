(ns sample2.foo
  "This namespace contains a read and write function that expect config as the first arg.")

(defn bar
  "This is the bar read method"
  [config request]
  {:body
   [:bar (:uri request) (:route-params request) config]})

(defn bar!
  "This is the bar write method"
  [config request]
  {:body
   [:bar! (:uri request) (:body request) (:route-params request) config]})
