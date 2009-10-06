(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "world")
	(amqp/bind-queue chan "world" "ex" "world")
	(amqp/subscribe chan "world" #(println (read-json-string %))))  
