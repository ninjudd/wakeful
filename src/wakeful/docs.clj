(ns wakeful.docs
  (:use compojure.core
        [hiccup core page-helpers]
        [useful.debug :only [?]])
  (:require [clojure.string :as s]))

(defn analyze-var [suffix var]
  (let [{fn-name :name :as var-meta} (-> var meta (select-keys [:name :arglists :doc :ns :params]))
        fn-name (str fn-name)
        [bare-name write?] (seq (.split fn-name
                                        (str "(?=" (java.util.regex.Pattern/quote suffix) "$)")))]
    (into var-meta {:include? (re-matches #"[\w-]+" bare-name)
                    :fn-name bare-name
                    :http-method (if write? "POST" "GET")
                    :write? (if write? :write :read)
                    :args (some var-meta [:params :arglists])})))


(defn group-by-method
  "Returns a map of :read and :write."
  [ns suffix]
  (->> ns symbol ns-publics vals
       (map (partial analyze-var suffix))
       (filter :include?)
       (group-by :write?)))

(defn generate-html
  "Generate HTML based on some information from metadata."
  [v ns-prefix]
  (let [{:keys [args doc http-method fn-name ns write?]} v]
    (html
     [:div.fn {:id (s/join "-" [(name write?)
                                (s/replace ns "." "-")
                                fn-name])}
      [:a {:name fn-name}]
      [:h3 fn-name]
      [:p
       [:span.method http-method]
       " "
       [:span.url (str "/" (subs (str ns) (inc (count ns-prefix))) "/" fn-name)]
       (when args [:p.args (pr-str args)])] ;; TODO customize arglists
      [:p.doc doc]])))

(defn build-page
  "Compose a documentation page."
  [ns & components] (html4 [:body [:h1 ns] (apply str components)]))

(defn generate-page
  "Generate HTML documentation for all the public methods in a group of namespaces
   under a prefix."
  [ns-prefix ns suffix]
  (let [{:keys [read write]} (group-by-method ns suffix)
        gen #(generate-html % ns-prefix)]
    (build-page
     ns
     (html
      [:h2 "Read"]
      (map gen read)
      [:h2 "Write"]
      (map gen write)))))

(defn ns-url [ns]
  (str "docs/" ns))

(defn generate-method-list
  "Creates anchors out of each of the items."
  [ns methods]
  (for [m methods]
    [:li [:a {:href (str (ns-url ns) "#" m)} m]]))

(defn generate-method-block [heading ns methods]
  (when-let [methods (seq (map :fn-name methods))]
    (html  [:p.route-type heading]
           [:ul (generate-method-list ns methods)])))

(defn generate-top
  "Generate top-level page."
  [ns-prefix suffix]
  (let [nss (filter (partial re-find (-> ns-prefix str re-pattern))
                    (map str (all-ns)))]
    (html4
     [:head (include-css "/css/docs.css")]
     [:body
      [:div#outer-container
       [:h1#ns-name ns-prefix]
       (for [ns nss]
         (html
          [:div.node-type [:h2 [:a {:href (ns-url ns)} ns]]
           (let [{read-methods :read write-methods :write} (group-by-method ns suffix)]
             (html (generate-method-block "writing" ns write-methods)
                   (generate-method-block "reading" ns read-methods)))]))]])))
