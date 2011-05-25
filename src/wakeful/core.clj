(ns wakeful.core
  (:use compojure.core
        [useful :only [update into-map]]
        [ring.middleware.params :only [wrap-params]]
        [clout.core :only [route-compile]])
  (:require [clj-json.core :as json]))

(defn resolve-method [ns-prefix type method method-suffix]
  (let [ns     (symbol (if type (str (name ns-prefix) "." type) ns-prefix))
        method (when-not (.endsWith method "*")
                 (symbol (str method method-suffix)))]
      (require ns)
      (or (ns-resolve ns method)
          (throw (UnsupportedOperationException.
                  (format "Unable to resolve method: %s/%s" ns method))))))

(defn node-type [^String id]
  (let [i (.indexOf id "-")]
    (when (not= -1 i)
      (.substring id 0 i))))

(defn- assoc-type [route-params]
  (assoc route-params :type (node-type (:id route-params))))

(defn- wrap-json [handler]
  (fn [{body :body :as request}]
    (let [body (when body (json/parse-string (slurp body)))]
      (-> (handler (assoc request :body body))
          (update :body json/generate-string)
          (assoc-in [:headers "Content-Type"] "application/json; charset=utf-8")))))

(defn- ns-router [ns-prefix & [method-suffix]]
  (fn [{{:keys [method type id]} :route-params :as request}]
    (let [method (resolve-method ns-prefix type method method-suffix)]
      (method request))))

(defn- read-routes [read]
  (routes (GET (route-compile "/:id" {:id #"\w+-\d+"}) {:as request}
               (read (-> request
                         (update :route-params assoc-type)
                         (assoc-in [:route-params :method] "node"))))

          (GET (route-compile "/:id/:method" {:id #"\w+-\d+"}) {:as request}
               (read (update request :route-params assoc-type)))

          (GET (route-compile "/:id/:method/*" {:id #"\w+-\d+"}) {:as request}
               (read (update request :route-params assoc-type)))

          (GET "/:type/:method" {:as request}
               (read request))

          (GET "/:type/:method/*" {:as request}
               (read request))

          (GET "/:method" {:as request}
               (read request))))

(defn- write-routes [write]
  (routes (POST (route-compile "/:id/:method" {:id #"\w+-\d+"}) {:as request}
                (write (update request :route-params assoc-type)))

          (POST (route-compile "/:id/:method/*" {:id #"\w+-\d+"}) {:as request}
                (write (update request :route-params assoc-type)))

          (POST "/:type/:method" {:as request}
                (write request))

          (POST "/:type/:method/*" {:as request}
                (write request))

          (POST "/:method" {:as request}
                (write request))))

(def *bulk* nil)

(defn- bulk [method handler]
  (fn [{:keys [body query-params]}]
    (binding [*bulk* true]
      {:body (doall
              (map (fn [[path params body]]
                     (:body (handler
                             {:request-method method
                              :uri            path
                              :query-params   (merge query-params (or params {}))
                              :body           body})))
                   body))})))

(defn- wrap [f wrap]
  (if wrap (wrap f) f))

(defn- bulk-routes [read write opts]
  (let [bulk-read  (wrap (bulk :get  read)  (:wrap-bulk-read  opts))
        bulk-write (wrap (bulk :post write) (:wrap-bulk-write opts))]
    (routes (POST "/bulk-read" {:as request}
                  (bulk-read request))
            (POST "/bulk-write" {:as request}
                  (bulk-write request)))))

(defn wakeful [ns-prefix & opts]
  (let [opts  (into-map opts)
        read  (read-routes  (wrap (ns-router ns-prefix)     (:wrap-read  opts)))
        write (write-routes (wrap (ns-router ns-prefix "!") (:wrap-write opts)))
        bulk  (bulk-routes read write opts)]
    (-> (routes read bulk write)
        wrap-params
        wrap-json)))
