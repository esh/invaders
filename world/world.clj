(ns world
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:use [clojure.contrib.json.read])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* 
	{ :classname "org.sqlite.JDBC"
	  :subprotocol "sqlite"
	  :subname "world.db" })

(def *user-possessions-atom* (atom {}))

(defn init-items []
	(with-connection *db* (with-query-results results ["select name from items"]
		(reduce (fn [items row] (assoc items (keyword (:name row)) 0)) {} results))))

(defn load-users []
	(let [items (init-items)]
		(with-connection *db* (with-query-results results ["select distinct owner from possessions"]
			(reduce (fn [users row] (assoc users (keyword (:owner row)) (ref items))) {} results)))))	

(defn load-possessions []
	(reset! *user-possessions-atom* (load-users))
	(with-connection *db* (with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [row results]
			(let [user (keyword (:owner row))
			      item (keyword (:item row))
			      qty (:qty row)
			      user-possession-ref (user @*user-possessions-atom*)]
				(println "loading " user)
				(dosync (commute user-possession-ref
						 (fn [m item qty] (assoc m item qty))
						 item qty)))))))


(load-possessions)

;listen to world
(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "world")
	(amqp/bind-queue chan "world" "ex" "world")
	(amqp/subscribe chan "world"
		#(println (read-json-string %))))  
