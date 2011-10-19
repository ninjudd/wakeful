(ns sample.bar
  "This sample namespace contains a single read and a single write funciton")

(defn baz [request]
  [:baz (:uri request) (:route-params request)])

(defn baz! [request]
  [:baz! (:uri request) (:body request) (:route-params request)])
