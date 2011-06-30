(ns wakeful.docs
  (:use compojure.core
        [hiccup core page-helpers]
        [clojure.string :only [join]]))

(defn extract-info
  "Extract important information from the meta map of a var."
  [x] (-> x meta (select-keys [:name :arglists :doc :ns])))

(defn include?
  "Determine whether a name should be listed in the documentation."
  [name suffix]
  (let [[bare-name] (.split name (str (java.util.regex.Pattern/quote suffix)
                                      "$"))]
    (re-matches #"[\w-]+" bare-name)))

(defn group-by-method
  "Returns a map of :read and :write."
  [ns suffix]
  (let [var-name (comp name :name meta)]
    (->> ns symbol ns-publics vals
         (filter #(include? (var-name %) suffix))
         (group-by
          #(if (.endsWith (var-name %) suffix)
             :write
             :read)))))

(defn generate-html
  "Generate HTML based on some information from metadata."
  [v ns-prefix]
  (let [{:keys [arglists doc name ns]} v]
    (html
     [:a {:name name}]
     [:h3 name]
     [:p (str "/" (subs (str ns) (inc (count ns-prefix))) "/" name)]
     (when arglists [:p (pr-str arglists)])
     [:p doc])))

(defn build-page
  "Compose a documentation page."
  [ns & components] (html4 [:body [:h1 ns] (apply str components)]))

(defn generate-page
  "Generate HTML documentation for all the public methods in a group of namespaces
   under a prefix."
  [ns-prefix ns suffix]
  (let [{:keys [read write]} (group-by-method ns suffix)
        gen #(-> % extract-info (generate-html ns-prefix))]
    (build-page
     ns
     (html
      [:h2 "Read"]
      (map gen read)
      [:h2 "Write"]
      (map gen write)))))

(defn ns-url [ns]
  (str "docs/" ns))

(defn anchor
  "Creates anchors out of each of the items."
  [ns items]
  (join " " (map #(html [:a {:href (str (ns-url ns) "#" %)} %]) items)))

(defn extract-name
  "Pull the name out of a var's metadata."
  [v] (-> v meta :name))

(defn generate-top
  "Generate top-level page."
  [ns-prefix suffix]
  (let [nss (filter (partial re-find (-> ns-prefix str re-pattern))
                    (map str (all-ns)))]
    (html4
     [:head (include-css "/css/docs.css")]
     [:body
      [:h1 ns-prefix]
      (for [ns nss]
        (html
         [:h2 [:a {:href (ns-url ns)} ns]]
         (let [{read-methods :read write-methods :write} (group-by-method ns suffix)
               name (partial map extract-name)]
           (when-let [read-methods (name read-methods)]
             (html [:h3 "Read:"] (anchor ns read-methods)))
           (when-let [write-methods (name write-methods)]
             (html [:h3 "Write:"] (anchor ns write-methods))))))])))
