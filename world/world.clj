(ns world
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:import (com.rabbitmq.client
			ConnectionParameters
			Connection
			Channel
			AMQP
			ConnectionFactory
			Consumer
			QueueingConsumer)))

(. Class (forName "org.sqlite.JDBC"))

(def *db* 
	{ :classname "org.sqlite.JDBC"
	  :subprotocol "sqlite"
	  :subname "world.db" })

(defn connect [host port username password virtualhost exchange-name exchange-type] 
	(let [params (doto (new ConnectionParameters)
			(.setUsername username)
			(.setPassword password)
			(.setVirtualHost virtualhost))
	      conn (.newConnection (new ConnectionFactory params) host port)
	      ch (.createChannel conn)]
		(.exchangeDeclare ch exchange-name exchange-type)
		ch))

(defn create-queue [channel queue]
	(.queueDeclare channel queue))

(defn bind-queue [channel queue exchange routing-key]
	(.queueBind channel queue exchange routing-key))

(defn publish [channel exchange routing-key message]
	(.basicPublish channel exchange routing-key nil (.getBytes message)))
		 
(defn snapshot [user]
	(with-connection *db*
		(with-query-results results ["select * from possessions"]
			(print results))))
		
