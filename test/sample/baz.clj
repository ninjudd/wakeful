(ns sample.baz
  "This is a sample namespace that only contains a single write action")

(defn write! [request]
  [:write! (:uri request) (:body request) (:route-params request)])
