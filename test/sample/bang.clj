(ns sample.bang
  "This namespace contains a single read function."
  (:refer-clojure :exclude [read]))

(defn read [request]
  [:read (:uri request) (:route-params request)])