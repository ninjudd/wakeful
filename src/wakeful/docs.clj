(ns wakeful.docs
  (:use compojure.core
        [hiccup core page-helpers]
        [clojure.string :only [join]]))

(defn extract-info
  "Extract important information from the meta map of a var."
  [x] (-> x meta (select-keys [:name :arglists :doc :ns])))

(defn group-by-method
  "Returns a map of :read and :write."
  [ns suffix]
  (->> ns symbol ns-publics vals
       (group-by
        #(if (.endsWith (str %) suffix)
           :write
           :read))))

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

(defn anchor
  "Creates anchors out of each of the items."
  [ns items]
  (join " "  (map #(html [:a {:href (str "/docs/" ns "#" %)} %]) items)))

(defn extract-name
  "Pull the name out of a var's metadata."
  [v] (-> v meta :name))

(defn generate-top
  "Generate top-level page."
  [ns-prefix suffix]
  (let [nss (filter (partial re-find (-> ns-prefix str re-pattern))
                    (map str (all-ns)))]
    (html4
     [:h1 "Namespaces under " ns-prefix]
     [:head (include-css "/css/docs.css")]
     [:body
      (for [ns nss]
        (html
         [:a {:href (str "/docs/" ns)} ns]
         [:br]
         (let [{:keys [read write]} (group-by-method ns suffix)
               name (partial map extract-name)]
           (html
            (str "Read: " (anchor ns (name read)))
            [:br]
            (str "Write: " (anchor ns (name write)))))
         [:br]
         [:br]))])))