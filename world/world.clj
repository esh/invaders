(ns world
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:use [clojure.contrib.json.read])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* 
	{ :classname "org.sqlite.JDBC"
	  :subprotocol "sqlite"
	  :subname "world.db" })
	
(def *user-possessions-map* (atom {}))

(defstruct possession-struct :user :possesions)

(defn create-user [user]
	(let [m @*user-possessions-map*
	      possessions (ref {})
	      key (keyword user)
              m (assoc m key possessions)]  
		(reset! *user-possessions-map* m)
		possessions)) 
 
(defn load-possessions []
	(with-connection *db* (with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(dorun (map (fn [row]
			(let [user (keyword (:owner row))
			      item (keyword (:item row))
			      qty (:qty row)]
				(if (not (contains? @*user-possessions-map* user))
              				(let [m (assoc @*user-possessions-map* user (ref {}))]  
						(reset! *user-possessions-map* m)))
					(let [m (user @*user-possessions-map*)]
						(println row)))) results)))))
