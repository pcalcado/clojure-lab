(ns httpasyncclient-test.core
  (:use clojure.contrib.server-socket)
  (:use net.cgrand.moustache)
  (:use [ring.adapter.jetty :only [run-jetty]])  
  (:use [ring.middleware file params session])
  (:require [clojure.contrib.duck-streams :as duck-streams])
  (:require [clojure.contrib.jmx :as jmx])
  (import [java.io BufferedReader InputStreamReader PrintWriter ]
          [java.util.concurrent Executors]
          [org.apache.http.client.methods HttpGet]
          [org.apache.http.impl.conn.tsccm ThreadSafeClientConnManager]
          [org.apache.http.impl.client DefaultHttpClient BasicResponseHandler]))

;;default client
(def max-connections 50)
(def http-client (DefaultHttpClient.
                   (doto (new ThreadSafeClientConnManager)
                     (.setMaxTotal max-connections)
                     (.setDefaultMaxPerRoute max-connections))))

(defn GET [uri] (. http-client execute (new HttpGet uri) (new BasicResponseHandler)))


;;util
(defn run-out-of-ports []
  (let [more-ports-than-allowed (+ 1
                                   (long (jmx/read "java.lang:type=OperatingSystem" :MaxFileDescriptorCount)))]
    (doall
     (. (Executors/newFixedThreadPool more-ports-than-allowed) invokeAll
        (map (fn [_] #(GET "http://localhost:8765/quick/1")) (range more-ports-than-allowed))))))

(def log-filename "ports.log")

(defn log [txt] (duck-streams/append-spit log-filename (str  txt "\n")))

;;server
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

;;TODO: apparently {:get [request-number "@"]} causes a "string is not fn" exception. bug in moustache?
(defn app-for [response-fn] (app [request-number]
                                 {:get ["" request-number "@" (response-fn)]}))

(def webservice-app (app
                     ["slow"  &] (app-for slow-response)
                     ["quick" &] (app-for quick-response)))

(defn start-server! []
  (. (Thread. #(run-jetty #'webservice-app {:port port})) start))

;;requests
(def number-of-threads 170)
(defn request! [endpoint request-number]
  (GET (str "http://localhost:" port "/" endpoint "/" request-number )))

(defn make-request [request-number] 
  (fn []    
    (let [data-from-slow-endpoint (request! "slow" request-number)
          data-from-quick-endpoint (map (fn [&_] (request! "quick" request-number)) (range 10))]
      (str (apply str data-from-quick-endpoint) data-from-slow-endpoint))))

(def dogpile (map make-request (range number-of-threads)))


;;strategies
;;serial
(defn serialised-dogpile! []
  (map #(apply % nil) dogpile))

;;naive-threads


;;threaded+futures
(defn threaded-dogpile! []
  (let [executor-service (Executors/newFixedThreadPool number-of-threads)]
    (. executor-service invokeAll dogpile)))

;;enhanced futures



;;apache http async?


;;ning?


;;benchmarks
(defmacro benchmark [& code]
  `(let [start# (System/currentTimeMillis)]
     (log (str "STARTED: " '~@code))
     (doall ~@code)
     (let [finish# (System/currentTimeMillis)]
       (log (str "FINISHED:" '~@code  "Took: " finish# "-" start# "=" (- finish# start#))))))

(defn dump-file-count! []
  (. (Thread.
      (fn []
        (loop []
          (log (str (System/currentTimeMillis) "," (jmx/read "java.lang:type=OperatingSystem" :OpenFileDescriptorCount)))
          (Thread/sleep 500)
          (recur))))     
     start))

(defn init-system! []
  (log "==>Starting")
  (start-server!)
  (dump-file-count!))

