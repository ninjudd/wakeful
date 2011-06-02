(ns wakeful.core
  (:use compojure.core
        [useful :only [update into-map verify]]
        [ring.middleware.params :only [wrap-params]]
        [clout.core :only [route-compile]]
        wakeful.docs)
  (:require [clj-json.core :as json]))

(defn foo [& all] "blah")

(defn resolve-method [ns-prefix type method]
  (let [ns     (symbol (if type (str (name ns-prefix) "." (name type)) ns-prefix))
        method (symbol (if (string? method) method (apply str method)))]
    (try (require ns)
         (ns-resolve ns method)
         (catch java.io.FileNotFoundException e))))

(defn node-type [^String id]
  (let [i (.indexOf id "-")]
    (when (not= -1 i)
      (.substring id 0 i))))

(defn node-number [^String id & [node-type]]
  (let [[type num] (.split id "-")]
    (verify (or (nil? node-type) (= node-type type))
            (format "node-id %s is not of type %s" id node-type))
    (Long/parseLong num)))

(defn- assoc-type [route-params]
  (assoc route-params :type (node-type (:id route-params))))

(defn- wrap-content-type [handler content-type]
  (fn [{body :body :as request}]
    (let [json? (.startsWith content-type "application/json")
          body
          (when body
            (if json?
              (json/parse-string (slurp body))
              body))]
      (when-let [response (handler (assoc request :body body :form-params {}))]
        (let [response (assoc-in response [:headers "Content-Type"] content-type)]
          (if json?
            (update response :body json/generate-string)
            response))))))

(defn- ns-router [ns-prefix wrapper & [method-suffix]]
  (fn [{{:keys [method type id]} :route-params :as request}]
    (when-let [method (resolve-method ns-prefix type [method method-suffix])]
      (if (and wrapper (not (:no-wrap (meta method))))
        ((wrapper method) request)
        (method request)))))

(defn route [pattern]
  (route-compile pattern {:id #"\w+-\d+" :type #"\w+" :method #"[\w-]+" :ns #".*"}))

(defn- read-routes [read]
  (routes (GET (route "/:id") {:as request}
               (read (-> request
                         (update :route-params assoc-type)
                         (assoc-in [:route-params :method] "node"))))

          (GET (route "/:id/:method") {:as request}
               (read (update request :route-params assoc-type)))

          (GET (route "/:id/:method/*") {:as request}
               (read (update request :route-params assoc-type)))

          (GET (route "/:type/:method") {:as request}
               (read request))

          (GET (route "/:type/:method/*") {:as request}
               (read request))

          (GET (route "/:method") {:as request}
               (read request))))

(defn- write-routes [write]
  (routes (POST (route "/:id/:method") {:as request}
                (write (update request :route-params assoc-type)))

          (POST (route "/:id/:method/*") {:as request}
                (write (update request :route-params assoc-type)))

          (POST (route "/:type/:method") {:as request}
                (write request))

          (POST (route "/:type/:method/*") {:as request}
                (write request))

          (POST (route "/:method") {:as request}
                (write request))))

(def *bulk* nil)

(defn- bulk [request-method handler wrapper]
  ((or wrapper identity)
   (fn [{:keys [body query-params]}]
     (binding [*bulk* true]
       {:body (doall
               (map (fn [[uri params body]]
                      (:body (handler
                              {:request-method request-method
                               :uri            uri
                               :query-params   (merge query-params (or params {}))
                               :body           body})))
                    body))}))))

(defn- bulk-routes [read write opts]
  (let [bulk-read  (bulk :get  read  (:bulk-read  opts))
        bulk-write (bulk :post write (:bulk-write opts))]
    (routes (POST "/bulk-read" {:as request}
                  (bulk-read request))
            (POST "/bulk-write" {:as request}
                  (bulk-write request)))))

(defn doc-routes [ns-prefix suffix]
  (routes (GET "/docs" []
               (generate-top ns-prefix suffix))
          
          (GET (route "/docs/:ns") {{ns :ns} :params}
               (generate-page ns suffix))))

(defn wakeful [ns-prefix & opts]
  (let [{:keys [docs? write-suffix content-type]
         :or {docs? true
              write-suffix "!"
              content-type "application/json; charset=utf-8"}
         :as opts} (into-map opts)

        suffix (or (:write-suffix opts) "!")
        read   (read-routes  (ns-router ns-prefix (:read  opts)))
        write  (write-routes (ns-router ns-prefix (:write opts) suffix))
        bulk   (bulk-routes read write opts)
        rs     (-> (routes read bulk write) wrap-params (wrap-content-type content-type))]
    (routes
     (when docs?
       (doc-routes ns-prefix suffix))
     rs)))
