(ns wakeful.core
  (:use compojure.core
        [useful :only [update into-map]]
        [ring.middleware.params :only [wrap-params]]
        [clout.core :only [route-compile]])
  (:require [clj-json.core :as json]))

(defn resolve-method [ns-prefix type method method-suffix]
  (let [ns     (symbol (if type (str ns-prefix "." type) ns-prefix))
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

(defn- router [ns-prefix & [wrap method-suffix]]
  (let [wrap (or wrap identity)]
    (-> (fn [{{:keys [method type id]} :route-params :as request}]
          (let [method (resolve-method ns-prefix type method method-suffix)]
            (method request)))
        wrap-params
        wrap-json
        wrap)))

(def *bulk* nil)

(defn- bulk [method handler & [wrap]]
  (fn [{:keys [body query-params]}]
    (binding [*bulk* true]
      {:body (doall (map (fn [{:strs [path params body]}]
                           (:body (handler
                                   {:request-method method
                                    :uri            path
                                    :query-params   (merge query-params params)
                                    :body           body})))
                         body))})))

(defn handler [ns-prefix & opts]
  (let [opts       (into-map opts)
        read       (router ns-prefix (:wrap-read opts))
        write      (router ns-prefix (:wrap-write opts) "!")
        bulk-read  (bulk :get  read  (:wrap-bulk-read opts))
        bulk-write (bulk :post write (:wrap-bulk-write opts))]
    (routes
     (GET (route-compile "/:id" {:id #"\w+-\d+"}) {:as request}
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
          (read request))

     (POST (route-compile "/:id/:method" {:id #"\w+-\d+"}) {:as request}
           (write (update request :route-params assoc-type)))

     (POST (route-compile "/:id/:method/*" {:id #"\w+-\d+"}) {:as request}
           (write (update request :route-params assoc-type)))

     (POST "/:type/:method" {:as request}
           (write request))

     (POST "/:type/:method/*" {:as request}
           (write request))

     (POST "/bulk-read" {:as request}
           (bulk-read request))

     (POST "/bulk-write" {:as request}
           (bulk-write request))

     (POST "/:method" {:as request}
           (write request)))))
