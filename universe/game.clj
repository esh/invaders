(ns game
	(:require [amqp])
	(:use [clojure.contrib.json.write]))

(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(.start (new Thread
		#((loop []
			(universe/mine-resources)
			(doseq [user (universe/get-online-users)] 
				(amqp/publish
					chan
					"ex"
					(str "client." user) 
					(json-str {:type "possessions" :payload (universe/get-possessions user)})))
			(. Thread sleep 60000)	
			(recur))))))
