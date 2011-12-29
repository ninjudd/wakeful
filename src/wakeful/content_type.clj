(ns wakeful.content-type
  (:use [useful.map :only [update]]
        [useful.fn :only [! fix]]
        [clojure.string :only [split]])
  (:require [clj-json.core :as json]))

(defn slurp-body [body]
  (fix body (! string?) slurp))

(defn content-type [_ content-type]
  (first (split content-type #"\s*;")))

(defmulti decode-body content-type)
(defmulti encode-body content-type)

(defmethod decode-body :default [body content-type] body)
(defmethod encode-body :default [body content-type] body)

(defmethod decode-body "application/json" [body content-type]
  (when body
    (-> body slurp-body json/parse-string)))

(defmethod encode-body "application/json" [body content-type]
  (try (json/generate-string body)
       (catch Exception e
         (Exception. (str "error JSON encoding body: " (prn-str body)) e))))

(defn wrap-content-type [handler content-type]
  (fn [request]
    (let [response (handler (-> (assoc request :form-params {})
                                (update :body decode-body content-type)))]
      (when response
        (-> (assoc-in response [:headers "Content-Type"] content-type)
            (update :body encode-body content-type))))))
