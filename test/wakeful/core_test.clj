(ns wakeful.core-test
  (:use clojure.test wakeful.core)
  (:require [clj-json.core :as json]))

(defn wrap-body [f]
  (fn [request]
    {:body (f request)}))

(deftest test-read
  (let [servlet  (handler "sample" :wrap-read wrap-body)]
    (let [response (servlet {:uri "/foo-1/foo" :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["foo" {"type" "foo", "method" "foo", "id" "foo-1"}]
             (json/parse-string (:body response)))))
    (let [response (servlet {:uri "/foo-8/bar" :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar" {"type" "foo", "method" "bar", "id" "foo-8"}]
             (json/parse-string (:body response)))))
    (let [response (servlet {:uri "/foo/bar" :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar" {"type" "foo", "method" "bar"}]
             (json/parse-string (:body response)))))))

(deftest test-top-level-read
  (let [servlet  (handler "sample" :wrap-read wrap-body)]
    (let [response (servlet {:uri "/a" :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["a" "/a" {"method" "a"}]
             (json/parse-string (:body response)))))))



