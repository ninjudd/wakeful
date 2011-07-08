(ns wakeful.utils)

(defn valid-ns? [prefix ns]
  (let [ns-name (str ns)]
    (and (.startsWith ns-name prefix)
         (not (re-find #"-test|test-" ns-name)))))

(def method-regex #"[\w-]+")

(defn parse-fn-name
  "Takes a wakeful function name and parses it. Returns the bare name, without extension, and :read, :write or nil."
  [fn-name write-suffix]
  (cond (.endsWith fn-name write-suffix)
        [(subs fn-name 0 (- (count fn-name) (count write-suffix))) :write]

        (re-matches method-regex fn-name)
        [fn-name :read]

        :else [fn-name nil]))

(defn analyze-fn
  "Returns an extended map of a vars meta-data"
  [write-suffix var]
  (let [meta (-> var meta (select-keys [:name :arglists :doc :ns :params]))
        [bare-name type] (parse-fn-name (str (:name meta)) write-suffix)]
    (assoc meta
      :bare-name bare-name
      :type type
      :args (some meta [:params :arglists]))))