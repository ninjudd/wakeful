(ns wakeful.core-test
  (:use clojure.test wakeful.core)
  (:require [clj-json.core :as json])
  (:import (java.io ByteArrayInputStream)))

(defn wrap-body [f]
  (fn [request]
    {:body (f request)}))

(defn json-stream [obj]
  (ByteArrayInputStream. (.getBytes (json/generate-string obj))))

(deftest test-read
  (let [handler (wakeful "sample" :read wrap-body)]
    (let [response (handler {:uri "/foo-1/foo", :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["foo" "/foo-1/foo" {"type" "foo", "method" "foo", "id" "foo-1"}]
             (json/parse-string (:body response)))))
    (let [response (handler {:uri "/foo-8/bar", :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar" "/foo-8/bar" {"type" "foo", "method" "bar", "id" "foo-8"}]
             (json/parse-string (:body response)))))
    (let [response (handler {:uri "/foo/bar", :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar" "/foo/bar" {"type" "foo", "method" "bar"}]
             (json/parse-string (:body response)))))))

(deftest test-top-level-read
  (let [handler (wakeful "sample" :read wrap-body)]
    (let [response (handler {:uri "/a" :request-method :get})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["a" "/a" {"method" "a"}]
             (json/parse-string (:body response)))))))

(deftest test-bulk-read
  (let [handler (wakeful "sample" :read wrap-body)]
    (let [response (handler {:uri "/bulk-read", :request-method :post,
                             :body (json-stream [["/foo-1/foo"]
                                                 ["/bar-10/baz"]
                                                 ["/foo/bar" {"a" 1, "b" 2}]])})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= [["foo" "/foo-1/foo"  {"method" "foo", "type" "foo", "id" "foo-1"}]
              ["baz" "/bar-10/baz" {"method" "baz", "type" "bar", "id" "bar-10"}]
              ["bar" "/foo/bar"    {"method" "bar", "type" "foo"}]]
             (json/parse-string (:body response)))))))

(deftest test-write
  (let [handler (wakeful "sample" :write wrap-body)]
    (let [response (handler {:uri "/foo-1/foo", :request-method :post, :body (json-stream {"foo" 1})})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["foo!" "/foo-1/foo" {"foo" 1} {"type" "foo", "method" "foo", "id" "foo-1"}]
             (json/parse-string (:body response)))))
    (let [response (handler {:uri "/foo-8/bar", :request-method :post})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar!" "/foo-8/bar" nil {"type" "foo", "method" "bar", "id" "foo-8"}]
             (json/parse-string (:body response)))))
    (let [response (handler {:uri "/foo/bar", :request-method :post, :body (json-stream {"bar" true})})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["bar!" "/foo/bar" {"bar" true} {"type" "foo", "method" "bar"}]
             (json/parse-string (:body response)))))))

(deftest test-top-level-write
  (let [handler (wakeful "sample" :write wrap-body)]
    (let [response (handler {:uri "/b" :request-method :post})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= ["b!" "/b" {"method" "b"}]
             (json/parse-string (:body response)))))))

(deftest test-bulk-write
  (let [handler (wakeful "sample" :write wrap-body)]
    (let [response (handler {:uri "/bulk-write", :request-method :post,
                             :body (json-stream [["/foo-1/foo" nil ["a" "b" "c"]]
                                                 ["/bar-10/baz"]
                                                 ["/foo/bar" {"a" 1, "b" 2} [1 2 true false]]])})]
      (is (= 200 (:status response)))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (is (= [["foo!" "/foo-1/foo"  ["a" "b" "c"]    {"method" "foo", "type" "foo", "id" "foo-1"}]
              ["baz!" "/bar-10/baz" nil              {"method" "baz", "type" "bar", "id" "bar-10"}]
              ["bar!" "/foo/bar"    [1 2 true false] {"method" "bar", "type" "foo"}]]
             (json/parse-string (:body response)))))))

(deftest test-invalid-routes
  (let [handler (wakeful "sample" :read wrap-body)]
    (is (= nil (handler {:request-method :get, :uri "/foo/bar*"})))
    (is (= nil (handler {:request-method :get, :uri "/foo?"})))
    (is (= nil (handler {:request-method :get, :uri "/foo/bar!"})))))

(deftest test-missing-routes
  (let [handler (wakeful "sample" :read wrap-body)]
    (is (= nil (handler {:request-method :get, :uri "/foo/bam"})))
    (is (= nil (handler {:request-method :get, :uri "/intricate"})))))

(deftest test-no-wrap
  (let [handler (wakeful "sample" :read wrap-body)]
    (is (= ["b!" "/b" {"method" "b"}]
           (json/parse-string (:body (handler {:request-method :get, :uri "/b"})))))))

(deftest test-resolve-method-prefix
  (is (= #'sample.foo/bar! (resolve-method "sample" :foo ["ba" "r" "!"]))))