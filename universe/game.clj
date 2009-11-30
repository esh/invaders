(ns game
	(:require [universe])
	(:require [amqp])
	(:use [clojure.contrib.json.read])
	(:use [clojure.contrib.json.write])
	(:use [clojure.walk]))

(defn reply [chan user msg] 
	(amqp/publish chan "ex" (str "client." user) (json-str msg)))

(defn broadcast [chan msg]
	(doseq [user (seq (universe/get-online-users))]
		(reply chan user msg)))		

(defmulti dispatch (fn [chan user msg] (keyword (:action msg))))

(defmethod dispatch :possessions [chan user msg] (reply chan user {:type "possessions" :payload (universe/get-possessions user)}))

(defmethod dispatch :ship-meta [chan user msg] (reply chan user {:type "ship-meta" :payload (universe/get-ship-meta)}))

(defmethod dispatch :universe [chan user msg] (reply chan user {:type "universe" :payload (universe/get-universe)}))

(defmethod dispatch :move [chan user msg]
	(let [res (universe/move-ship user (:from msg) (:to msg))
	      uni (universe/get-universe)]
		(if (not (nil? res))
			(broadcast chan {:type "universe" :payload uni}))))

(defmethod dispatch :create [chan user msg]
	(let [res (universe/create-ship user (:type msg) (:position msg))
	      uni (universe/get-universe)]
		(if (not (nil? res))
			(broadcast chan {:type "universe" :payload uni}))))


(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")]
	;game loop
	(let [chan (amqp/create-channel conn "ex" "topic")]
		(.start (new Thread
			#((loop []
				(let [online-users (universe/get-online-users)] 
					(doseq [ship-ref @universe/*ships-atom*]
						(let [owner (:owner @ship-ref)
						      res (universe/ship-step ship-ref)]
							(if (contains? online-users owner)
								(reply chan owner {:type "possessions" :payload res}))))) 	
				(. Thread sleep 60000)	
				(recur))))))

	;listen to universe
	(let [chan (amqp/create-channel conn "ex" "topic")]
		(amqp/declare-queue chan "universe")
		(amqp/bind-queue chan "universe" "ex" "universe")
		(amqp/subscribe chan "universe"
			(fn [msg]
				(println msg)
				(let [msg (keywordize-keys (read-json-string msg))
				      user (:user msg)]
					(dispatch chan user msg))))))

(println "game server started")
