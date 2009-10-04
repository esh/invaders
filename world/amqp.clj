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

(defn declare-queue [channel queue-name]
	(.queueDeclare channel queue-name))

(defn bind-queue [channel queue-name exchange-name routing-key]
	(.queueBind channel queue-name exchange-name routing-key))

(defn publish [channel exchange-name routing-key message]
	(.basicPublish channel exchange-name routing-key nil (.getBytes message)))

(defn subscribe [channel queue-name fn]
	(let [consumer (new QueueingConsumer channel)]
		(.basicConsume channel queue-name, false, consumer)
		(loop [delivery (.nextDelivery consumer)]
			(try
				(fn (new String (.getBody delivery)))
				(.basicAck channel (.getDeliveryTag (.getEnvelope delivery)) false)
				(catch Exception e
					(.printStackTrace e)))
			(recur (.nextDelivery consumer)))))
