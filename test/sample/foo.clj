(ns sample.foo)

(defn foo [request]
  [:foo (:uri request) (:route-params request)])

(defn foo! [request]
  [:foo! (:uri request) (:body request) (:route-params request)])

(defn bar [request]
  [:bar (:uri request) (:route-params request)])

(defn bar! [request]
  [:bar! (:uri request) (:body request) (:route-params request)])