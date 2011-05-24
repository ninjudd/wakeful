(ns sample.foo)

(defn foo [{params :route-params}]
  [:foo params])

(defn bar [{params :route-params}]
  [:bar params])