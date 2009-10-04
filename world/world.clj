(ns world
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:use [clojure.contrib.json.read])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* 
	{ :classname "org.sqlite.JDBC"
	  :subprotocol "sqlite"
	  :subname "world.db" })
	 
(defn snapshot [user]
	(with-connection *db*
		(with-query-results results ["select * from possessions"]
			(print results))))

(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "world")
	(amqp/bind-queue chan "world" "ex" "world")
	(amqp/subscribe chan "world" #(println (read-json-string %))))  
