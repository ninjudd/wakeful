(ns wakeful.docs
  "Generates html documentation for a wakeful api"
  (:use compojure.core
        wakeful.util
        [hiccup core page-helpers]
        [useful.debug :only [?]])
  (:require [clojure.string :as s]))

(defn analyze-fn
  "Returns an extended map of a vars meta-data"
  [write-suffix var]
  (let [meta (-> var meta (select-keys [:name :arglists :doc :ns :params]))
        [bare-name type] (parse-fn-name (str (:name meta)) write-suffix)]
    (assoc meta
      :bare-name bare-name
      :type type
      :args (some meta [:params :arglists]))))

(defn meta->html
  "Turns a var's documentation meta-data into hiccup html"
  [doc-meta]
  [:div.rounded-box
   [:a {:name (:name doc-meta)}]
   [:h2 (:bare-name doc-meta)]
   [:span.title "params:"]
   [:span.code-block (str (:args doc-meta))]
   [:p (:doc doc-meta)]])

(defn group-by-type [ns write-suffix]
  (->> ns symbol ns-publics vals
       (map (partial analyze-fn write-suffix))
       (group-by :type)))

(defn generate-ns-docs
  "Generate the documentation for the specified ns"
  [ns-prefix ns write-suffix]
  (let [methods (group-by-type ns write-suffix)
        docs-for-type (fn [type methods]
                        (when-let [methods (seq (get methods type))]
                          (list [:h3.route-type (str (name type) " methods")]
                                (for [method methods]
                                  (meta->html method)))))]
    (html4
     [:head (include-css "../css/docs.css")]
     [:body
      [:div#outer-container
       [:h1 ns]
       [:p (:doc (meta (find-ns (symbol ns))))]
       (docs-for-type :write methods)
       (docs-for-type :read methods)]])))

(defn ns-url [ns]
  (str "docs/" ns))

(defn generate-method-block
  "Create an HTML list of the methods contained in the specified ns"
  [heading ns methods]
  (when-let [methods (seq (sort-by :name methods))]
    (list [:p.route-type heading]
          [:ul (for [method methods]
                 [:li [:a
                       {:href (str (ns-url ns) "#" (:name method))}
                       (:bare-name method)]])])))

(defn generate-top
  "Generate top-level page."
  [ns-prefix write-suffix]
  (let [nss (filter (partial re-find (-> ns-prefix str re-pattern))
                    (map str (all-ns)))]
    (html4
     [:head (include-css "css/docs.css")]
     [:body
      [:div#outer-container
       [:h1#main-ns ns-prefix]
       (for [ns nss]
         [:div.rounded-box [:h2 [:a {:href (ns-url ns)} ns]]
          [:p (:doc (meta (find-ns (symbol ns))))]
          (let [{read-methods :read write-methods :write} (group-by-type ns write-suffix)]
            (list (generate-method-block "writing" ns write-methods)
                   (generate-method-block "reading" ns read-methods)))])]])))
