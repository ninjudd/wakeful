(ns sample.bar)

(defn baz [request]
  [:baz (:uri request) (:route-params request)])

(defn baz! [request]
  [:baz! (:uri request) (:body request) (:route-params request)])
