from amqplib import client_0_8 as amqp

conn = amqp.Connection(host="localhost:5672 ", userid="guest",
	password="guest", virtual_host="/", insist=False)
chan = conn.channel()

msg = amqp.Message("hello world")
chan.basic_publish(msg, exchange="ex", routing_key="exchange")
