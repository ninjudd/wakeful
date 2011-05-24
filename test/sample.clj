(ns sample)

(defn a [request]
  [:a (:uri request) (:route-params request)])