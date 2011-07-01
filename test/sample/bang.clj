(ns sample.bang
  (:refer-clojure :exclude [read]))

(defn read [request]
  [:read (:uri request) (:route-params request)])