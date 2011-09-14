(ns wakeful.core
  (:use wakeful.docs wakeful.utils compojure.core
        [wakeful.content-type :only [wrap-content-type]]
        [compojure.route :only [files]]
        [clout.core :only [route-compile]]
        [useful.utils :only [verify]]
        [useful.map :only [update into-map map-keys-and-vals keyed]]
        [useful.io :only [resource-stream]]
        [useful.debug :only [?]]
        [ego.core :only [type-name]]
        [ring.middleware.params :only [wrap-params]]
        [clojure.string :only [join split]]
        [clojure.tools.namespace :only [find-namespaces-on-classpath]])
  (:require [useful.dispatch :as dispatch]))

(defmacro READ [& forms]
  `(fn [request#]
     (or ((GET  ~@forms) request#)
         ((HEAD ~@forms) request#))))

(defmacro WRITE [& forms]
  `(fn [request#]
     (or ((POST ~@forms) request#)
         ((PUT  ~@forms) request#))))

(defn assoc-type [route-params]
  (assoc route-params :type (type-name (:id route-params))))

(defn- route [pattern]
  (route-compile pattern {:id #"\w+-\d+" :type #"\w+" :method method-regex :ns #".*"}))

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

(defn- doc-routes [root suffix]
  (routes (GET "/docs" []
               (generate-top root suffix))
          (GET (route "/css/docs.css") []
               {:body (slurp (resource-stream "docs.css"))
                :headers {"Content-Type" "text/css"}})
          (GET (route "/docs/:ns") {{ns :ns} :params}
               (generate-ns-docs root ns suffix))))

(defn- conjoin [sep & coll]
  (join sep (remove nil? coll)))

(defn dispatcher [& opts]
  (let [{:keys [root hierarchy wrap prefix suffix default]
         :or {default (with-meta (constantly nil) {:no-wrap true})}} (into-map opts)
        hierarchy (map-keys-and-vals hierarchy #(symbol (conjoin "." root (name %))))]
    (dispatch/dispatcher (fn [{{:keys [method type]} :route-params action :action}]
                           (let [[type method] (if action
                                                 (split action #"\.")
                                                 [type method])]
                             (symbol (conjoin "." root type)
                                     (str prefix method suffix))))
                         (keyed [default hierarchy wrap]))))

(defn wakeful [& opts]
  (let [{:keys [root docs? write-suffix content-type auto-require? hierarchy read write]
         :or {docs?         true
              auto-require? true
              write-suffix  "!"
              content-type  "application/json; charset=utf-8"}
         :as opts} (into-map opts)
         dispatch  (partial dispatcher :root root :hierarchy hierarchy :wrap)
         read      (read-routes  (dispatch read))
         write     (write-routes (dispatch write :suffix write-suffix))
         bulk      (bulk-routes read write opts)
         docs      (when docs? (doc-routes root write-suffix))
         rs        (-> (routes read bulk write) wrap-params (wrap-content-type content-type))]
  (when auto-require?
    (doseq [ns (find-namespaces-on-classpath) :when (valid-ns? root ns)]
      (require ns)))
  (routes rs docs)))
