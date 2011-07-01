(ns sample.baz)

(defn write! [request]
  [:write! (:uri request) (:body request) (:route-params request)])
