(ns game
	(:require [amqp])
	(:use [clojure.contrib.json.write]))

(defn gameloop []
	(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      	      chan (amqp/create-channel conn "ex" "topic")]
		(do
			(universe/mine-resources)
			(doseq [user (universe/get-online-users)] 
				(amqp/publish
					chan
					"ex"
					(str "client." user) 
					(json-str (universe/get-possessions user)))))))
