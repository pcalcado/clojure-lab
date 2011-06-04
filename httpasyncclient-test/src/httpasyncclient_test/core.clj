(ns httpasyncclient-test.core
  (:use clojure.contrib.server-socket)
  (:use net.cgrand.moustache)
  (:use [ring.adapter.jetty :only [run-jetty]])  
  (:use [ring.middleware file params session])
  (import [java.io BufferedReader InputStreamReader PrintWriter ]
          [java.util.concurrent Executors]
          [org.apache.http.client.methods HttpGet]
          [org.apache.http.impl.conn.tsccm ThreadSafeClientConnManager]
          [org.apache.http.impl.client DefaultHttpClient BasicResponseHandler]))

;server
(def some-long-time (long 2000))
(def port 8765)

(defn slow-response []
  (println "slow")
  (Thread/sleep some-long-time)  
  (System/currentTimeMillis))

(defn quick-response []  
  (println "quick")
  (Thread/sleep (/ some-long-time 10))
  (System/currentTimeMillis))

;TODO: apparently {:get [request-number "@"]} causes a "string is not fn" exception. bug in moustache?
(defn app-for [response-fn] (app [request-number]
                                 {:get ["" request-number "@" (response-fn)]}))

(def webservice-app (app
                     ["slow"  &] (app-for slow-response)
                     ["quick" &] (app-for quick-response)))

(defn start-server! []
  (. (Thread. #(run-jetty #'webservice-app {:port port})) start))

;default client
(def number-of-threads 170)
(def http-client (DefaultHttpClient.
                   (doto (new ThreadSafeClientConnManager)
                     (.setMaxTotal number-of-threads)
                     (.setDefaultMaxPerRoute number-of-threads))))

(defn GET [uri] (. http-client execute (new HttpGet uri) (new BasicResponseHandler)))

;requests
(defn request! [endpoint request-number]
  (GET (str "http://localhost:" port "/" endpoint "/" request-number )))

(defn make-request [request-number] 
  (fn []    
    (let [data-from-slow-endpoint (request! "slow" request-number)
          data-from-quick-endpoint (map (fn [&_] (request! "quick" request-number)) (range 10))]
      (str (apply str data-from-quick-endpoint) data-from-slow-endpoint))))

(def dogpile (map make-request (range number-of-threads)))

;util

(defmacro timed [& code]
  `(let [start# (System/currentTimeMillis)]
     ~@code
     (let [finish# (System/currentTimeMillis)]
       (println "Took: " finish# "-" start# "=" (- finish# start#)))))

(defmacro benchmark [& code]
  `(doall (timed ~@code)))

;strategies
;serial
(defn serialised-dogpile! []
  (map #(apply % nil) dogpile) :over)
;Took:  1307120445540 - 1307119759931 = 685609

;threaded+futures
(defn threaded-dogpile! []
  (let [executor-service (Executors/newFixedThreadPool number-of-threads)]
    (. executor-service invokeAll dogpile)))
;per route = number-of-threads   => Took:  1307121685128 - 1307121679647 = 5481
;per route = number-of-threads/2 => Took:  1307122131996 - 1307122121225 = 10771
;per route = 2                   => Took:  1307122084873 - 1307121742519 = 342354

;enhanced futures
(doall (. (Executors/newFixedThreadPool 2000) invokeAll (map (fn [_] #(GET "http://localhost:8765/quick/1")) (range 20000))))

;apache http async?


;ning?

