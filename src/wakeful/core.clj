(ns wakeful.core
  (:use compojure.core
        [compojure.route :only [files]]
        [useful.utils :only [verify]]
        [useful.map :only [update into-map]]
        [useful.io :only [resource-stream]]
        [ring.middleware.params :only [wrap-params]]
        wakeful.docs
        wakeful.util
        clojure.tools.namespace))

(defmacro READ [& forms]
  `(fn [request#]
     (or ((GET  ~@forms) request#)
         ((HEAD ~@forms) request#))))

(defmacro WRITE [& forms]
  `(fn [request#]
     (or ((POST ~@forms) request#)
         ((PUT  ~@forms) request#))))

(defn- read-routes [read]
  (routes (READ (route "/:id") {:as request}
                (read (-> request
                          (update :route-params assoc-type)
                          (assoc-in [:route-params :method] "node"))))

          (READ (route "/:id/:method") {:as request}
                (read (update request :route-params assoc-type)))

          (READ (route "/:id/:method/*") {:as request}
                (read (update request :route-params assoc-type)))

          (READ (route "/:type/:method") {:as request}
                (read request))

          (READ (route "/:type/:method/*") {:as request}
                (read request))

          (READ (route "/:method") {:as request}
                (read request))))

(defn- write-routes [write]
  (routes (WRITE (route "/:id/:method") {:as request}
                 (write (update request :route-params assoc-type)))

          (WRITE (route "/:id/:method/*") {:as request}
                 (write (update request :route-params assoc-type)))

          (WRITE (route "/:type/:method") {:as request}
                 (write request))

          (WRITE (route "/:type/:method/*") {:as request}
                 (write request))

          (WRITE (route "/:method") {:as request}
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

(defn good-ns? [prefix ns]
  (let [sns (str ns)]
    (and (.startsWith sns prefix)
         (not (re-find #"-test|test-" sns)))))

(defn- auto-require [prefix]
  (doseq [ns (filter (partial good-ns? prefix) (find-namespaces-on-classpath))]
    (require ns)))

(defn doc-routes [ns-prefix suffix]
  (auto-require ns-prefix)
  (routes (GET "/docs" []
               (generate-top ns-prefix suffix))
          (GET (route "/css/docs.css") []
               {:body (slurp (resource-stream "docs.css"))
                :headers {"Content-Type" "text/css"}})
          (GET (route "/docs/:ns") {{ns :ns} :params}
               (generate-ns-docs ns-prefix ns suffix))))

(defn wakeful [ns-prefix & opts]
  (let [{:keys [docs? write-suffix content-type auto-require]
         :or {docs? true
              write-suffix "!"
              content-type "application/json; charset=utf-8"
              auto-require false}
         :as opts} (into-map opts)
        read   (read-routes  (ns-router ns-prefix (:read  opts)))
        write  (write-routes (ns-router ns-prefix (:write opts) write-suffix))
        bulk   (bulk-routes read write opts)
        rs     (-> (routes read bulk write) wrap-params (wrap-content-type content-type))]
    (when auto-require (auto-require ns-prefix))
    (routes
     (when docs? (doc-routes ns-prefix write-suffix))
     rs)))
