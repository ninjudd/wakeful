(ns wakeful.util
  (:use [ego.core :only [split-id type-name]]
        [useful.fn :only [transform-if]]
        [useful.map :only [update into-map]]
        [clout.core :only [route-compile]])
  (:require [clj-json.core :as json]))

(defn resolve-method [ns-prefix type method]
  (let [ns     (symbol (if type (str (name ns-prefix) "." (name type)) ns-prefix))
        method (symbol (if (string? method) method (apply str method)))]
    (try (require ns)
         (ns-resolve ns method)
         (catch java.io.FileNotFoundException e))))

(defn assoc-type [route-params]
  (assoc route-params :type (type-name (:id route-params))))

(defn wrap-content-type [handler content-type]
  (let [json? (.startsWith content-type "application/json")
        slurp (transform-if (complement string?) slurp)
        [fix-request fix-response] (if json?
                                     [#(when % (-> % slurp json/parse-string))
                                      #(update % :body json/generate-string)]
                                     [identity identity])]
    (fn [{body :body :as request}]
      (when-let [response (handler (assoc request :body (fix-request body) :form-params {}))]
        (fix-response (assoc-in response [:headers "Content-Type"] content-type))))))

(defn ns-router [ns-prefix wrapper & [method-suffix]]
  (fn [{{:keys [method type id]} :route-params :as request}]
    (when-let [method (resolve-method ns-prefix type [method method-suffix])]
      (if (and wrapper (not (:no-wrap (meta method))))
        ((wrapper method) request)
        (method request)))))

(def method-regex #"[\w-]+")

(defn route [pattern]
  (route-compile pattern {:id #"\w+-\d+" :type #"\w+" :method method-regex :ns #".*"}))

(defn parse-fn-name
  "Takes a wakeful function name and parses it. Returns the bare name, without extension, and :read, :write or nil."
  [fn-name write-suffix]
  (cond (.endsWith fn-name write-suffix)
        [(subs fn-name 0 (- (count fn-name) (count write-suffix))) :write]

        (re-matches method-regex fn-name)
        [fn-name :read]

        :else [fn-name nil]))
