(use 'ring.adapter.jetty 'wakeful.core)

(def handler (wakeful "sample" :content-type "text/html"))

(defn run [] (run-jetty (var handler) {:port 8080 :join? false}))

(defn do-run [] (run))