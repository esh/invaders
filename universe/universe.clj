(ns universe
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:use [clojure.contrib.json.read])
	(:use [clojure.contrib.json.write])
	(:use [clojure.walk])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "universe.db"})

(def *items* (with-connection *db* (with-query-results results ["select name from items"]
	(reduce (fn [items row] (assoc items (keyword (:name row)) 0)) {} results))))

(def *ships* (with-connection *db* (with-query-results results ["select name, description from ships"]
	(reduce (fn [ships row] (assoc ships (keyword (:name row)) row)) {} results))))

(def *possessions-atom* (atom {}))

(defn load-possessions []
	(reset! *possessions-atom* 
		(with-connection *db* (with-query-results results ["select distinct owner from possessions"]
				(reduce (fn [users row] (assoc users (keyword (:owner row)) (ref *items*))) {} results))))	
	(with-connection *db* (with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [row results]
			(let [user (keyword (:owner row))
			      item (keyword (:item row))
			      qty (:qty row)
			      user-possession-ref (user @*possessions-atom*)]
				(dosync (commute user-possession-ref
						 (fn [m item qty] (assoc m item qty))
						 item qty)))))))

(load-possessions)

;listen to universe
(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "universe")
	(amqp/bind-queue chan "universe" "ex" "universe")
	(amqp/subscribe chan "universe"
		(fn [msg]
			(let [msg (keywordize-keys (read-json-string msg))
			      user (:user msg)
			      snapshot (assoc
				@((keyword user) @*possessions-atom*)
				:user user :type "snapshot")]
				(println snapshot)		
				(amqp/publish chan "ex" (str "client." user) (json-str snapshot)))))) 
