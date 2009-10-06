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

(defn load-possessions []
	(let [results (with-connection *db* (with-query-results results ["select name from items"]))
	      items (reduce (fn [items row] (assoc items (keyword (:name row)) 0)) {} results)]
		(with-connection *db* (with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
			(doseq [row results]
				(let [user (keyword (:owner row))
			      	      item (keyword (:item row))
				      qty (:qty row)]
					(if (not (contains? @*user-possessions-map* user))
              					(let [m (assoc @*user-possessions-map* user (ref {}))]  
							(reset! *user-possessions-map* m)))
						(let [m (user @*user-possessions-map*)]
							(println row))))))))
