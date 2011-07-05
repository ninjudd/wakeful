(ns wakeful.docs
  "Generates html documentation for a wakeful api"
  (:use compojure.core
        [hiccup core page-helpers]
        [useful.debug :only [?]])
  (:require [clojure.string :as s]
            [wakeful.core :as core]))

(defn parse-fn-name [fn-name write-suffix]
  (cond (.endsWith fn-name write-suffix)
        [(subs fn-name 0 (- (count fn-name) (count write-suffix))) :write]

        (re-matches core/method-regex fn-name)
        [fn-name :read]

        :else [fn-name nil]))

(defn analyze-fn [write-suffix var]
  (let [meta (-> var meta (select-keys [:name :arglists :doc :ns :params]))
        [bare-name type] (parse-fn-name (str (:name meta)) write-suffix)]
    (assoc meta
      :bare-name bare-name
      :type type
      :args (some meta [:params :arglists]))))

(defn get-ns-fns [ns]
  (->> ns symbol ns-publics vals))

(defn meta->html [method]
  [:div.rounded-box
   [:h2 (:bare-name method)]
   [:span.title "params:"]
   [:span.code-block (str (:args method))]
   [:p (:doc method)]])

(defn generate-docs-for-type [type methods]
  (list [:h3.route-type (str (name type) " methods")]
        (for [method (get methods type)]
          (meta->html method))))

(defn generate-ns-docs [ns-prefix ns write-suffix]
  (let [methods (->> (get-ns-fns ns)
                     (map (partial analyze-fn write-suffix))
                     (group-by :type))]
    (html4
     [:head (include-css "../css/docs.css")]
     [:body
      [:div#outer-container
       [:h1 ns]
       [:p (:doc (meta (find-ns (symbol ns))))]
       (generate-docs-for-type :read methods)
       (generate-docs-for-type :write methods)]])))

(defn ns-url [ns]
  (str "docs/" ns))

(defn generate-method-list
  "Creates anchors out of each of the items."
  [ns methods]
  [:ul
   (for [m methods]
     [:li [:a {:href (str (ns-url ns) "#" m)} m]])])

(defn generate-method-block [heading ns methods]
  (when-let [methods (seq (sort (map :fn-name methods)))]
    (list  [:p.route-type heading]
           (generate-method-list ns methods))))

(defn generate-top
  "Generate top-level page."
  [ns-prefix suffix]
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
          (let [{read-methods :read write-methods :write} (group-by-method ns suffix)]
             (list (generate-method-block "writing" ns write-methods)
                   (generate-method-block "reading" ns read-methods)))])]])))
