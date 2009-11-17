(ns game
	(:require [universe])
	(:require [amqp])
	(:use [clojure.contrib.json.read])
	(:use [clojure.contrib.json.write])
	(:use [clojure.walk]))

(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(.start (new Thread
		#((loop []
;			(universe/mine-resources)
;			(doseq [user (universe/get-online-users)] 
;				(amqp/publish
;					chan
;					"ex"
;					(str "client." user) 
;					(json-str {:type "possessions" :payload (universe/get-possessions user)})))
			(. Thread sleep 60000)	
			(recur))))))


(defmulti dispatch #(keyword (:action %)))
(defmethod dispatch :possessions [msg] {:type "possessions" :payload (universe/get-possessions (:user msg))})
(defmethod dispatch :universe [msg] {:type "universe" :payload (universe/get-universe)})

;listen to universe
(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "universe")
	(amqp/bind-queue chan "universe" "ex" "universe")
	(amqp/subscribe chan "universe"
		(fn [msg]
			(let [msg (keywordize-keys (read-json-string msg))
			      user (:user msg)
			      res (dispatch msg)]
				(println res)
				(amqp/publish chan "ex" (str "client." user) (json-str res)))))) 
