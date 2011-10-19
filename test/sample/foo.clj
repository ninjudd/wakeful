(ns sample.foo
  "This namespace contains two read and two write functions.")

(defn foo
  "This is the foo read method"
  [request]
  [:foo (:uri request) (:route-params request)])

(defn foo!
  "This is the foo write method"
  [request]
  [:foo! (:uri request) (:body request) (:route-params request)])

(defn bar
  "This is the bar read method"
  [request]
  [:bar (:uri request) (:route-params request)])

(defn bar!
  "This is the bar write method"
  [request]
  [:bar! (:uri request) (:body request) (:route-params request)])