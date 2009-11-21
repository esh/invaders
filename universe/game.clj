(ns game
	(:require [universe])
	(:require [amqp])
	(:use [clojure.contrib.json.read])
	(:use [clojure.contrib.json.write])
	(:use [clojure.walk]))


(defmulti dispatch #(keyword (:action %)))
(defmethod dispatch :possessions [msg] {:type "possessions" :payload (universe/get-possessions (:user msg))})
(defmethod dispatch :universe [msg] {:type "universe" :payload (universe/get-universe)})

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
								(amqp/publish
									chan
									"ex"
									(str "client." (:owner @ship-ref)) 
									(json-str {:type "possessions" :payload res)})))))
				(. Thread sleep 60000)	
				(recur))))))

	;listen to universe
	(let [chan (amqp/create-channel conn "ex" "topic")]
		(amqp/declare-queue chan "universe")
		(amqp/bind-queue chan "universe" "ex" "universe")
		(amqp/subscribe chan "universe"
			(fn [msg]
				(let [msg (keywordize-keys (read-json-string msg))
				      user (:user msg)
				      res (dispatch msg)]
					(println res)
					(amqp/publish chan "ex" (str "client." user) (json-str res))))))) 
