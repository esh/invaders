(ns world
	(:use [clojure.contrib.sql :only (with-connection with-query-results)]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* 
	{ :classname "org.sqlite.JDBC"
	  :subprotocol "sqlite"
	  :subname "world.db" })

(defn snapshot [user]
	(with-connection *db*
		(with-query-results results ["select * from possessions"]
			(print results))))
		
