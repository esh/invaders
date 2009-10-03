(ns amqp 
	(:import (com.rabbitmq.client
			ConnectionParameters
			Connection
			Channel
			AMQP
			ConnectionFactory
			Consumer
			QueueingConsumer)))

(defn connect [host port username password virtualhost] 
	(let [params (doto (new ConnectionParameters)
			(.setUsername username)
			(.setPassword password)
			(.setVirtualHost virtualhost))]
		(.newConnection (new ConnectionFactory params) host port)))

(defn create-channel [conn exchange-name exchange-type]
	(let [ch (.createChannel conn)]
		(.exchangeDeclare ch exchange-name exchange-type)
		ch))	

(defn create-queue [channel queue]
	(.queueDeclare channel queue))

(defn bind-queue [channel queue exchange routing-key]
	(.queueBind channel queue exchange routing-key))

(defn publish [channel exchange routing-key message]
	(.basicPublish channel exchange routing-key nil (.getBytes message)))

(defn subscribe [channel queue fn]
	(let [consumer (new QueueingConsumer channel)]
		(.basicConsume channel queue, true, consumer)
		(loop [delivery (.nextDelivery consumer)]

			(recur (.nextDelivery consumer)))))
