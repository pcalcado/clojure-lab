(ns barulho.core
  (:use [com.mefesto.wabbitmq])
  (:use [com.mefesto.wabbitmq content-type])
  (:use [clojure.data json]))

(defn log [& msg] (println "LOG [" (System/currentTimeMillis) "]" msg))

(defn search-twitter! [& _]
  (read-json (slurp "http://search.twitter.com/search.json?q=soundcloud&result_type=recent")))

(defn links-in-stream [twitter-stream]
  (map #(second (re-matches #"^.*(http://\S+)\s*" %))
       (filter #(.contains % "http" ) (map :text (:results twitter-stream)))))

;;rabbitmq stuff
(def server-config {:host "localhost" :username "guest" :password "guest"})
(def links-from-twitter "links-from-twitter")
(def barulho-exchange-name "barulho.exchange")

(defmacro with-barulho [& action]
  `(with-broker server-config
     (with-channel {:content-types application-json}
       (with-exchange barulho-exchange-name
         ~@action))))
  
(defn send-message [queue message]
 (log "sending [" message "] -> " queue)
 (with-barulho
   (publish queue (.getBytes message))))

(defn setup-rabbit-mq! []
  (with-broker server-config
    (with-channel {:content-types application-json}
      (let [links-from-twitter-queue-name (str links-from-twitter ".queue")]
        (exchange-declare barulho-exchange-name "direct")
        (queue-declare links-from-twitter-queue-name)
        (queue-bind links-from-twitter-queue-name barulho-exchange-name links-from-twitter)))))

(defn start-producer! []
  (log "Producer starting...")
  (dotimes [_ 100]
    (let [links (remove nil? (links-in-stream (search-twitter!)))]
      (log "Sending... \n" links)
      (doseq [message (map (fn [l] (str "{:link \"" l  "\"}")) links)] (send-message links-from-twitter message)))
    (Thread/sleep 60000)))

(defn start-consumer! []
  (log "Consumer starting...")
  (with-barulho    
    (with-queue (str links-from-twitter ".queue")
      (doseq [msg (consuming-seq true)] ;
        (log msg)
        (println "received:" (String. (:body msg)))))))

;;main
(defn main []
  (setup-rabbit-mq!)
  (doto (Thread. #(start-producer!)) .start)
  (doto (Thread. #(start-consumer!)) .start))
