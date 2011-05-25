(ns sample)

(defn a [request]
  [:a (:uri request) (:route-params request)])

(defn b! [request]
  [:b! (:uri request) (:route-params request)])