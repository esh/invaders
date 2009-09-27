from amqplib import client_0_8 as amqp
from threading import Event
import simplejson as json
import random

conn = amqp.Connection(host="localhost:5672 ", userid="guest",
	password="guest", virtual_host="/", insist=False)
chan = conn.channel()
chan.queue_declare(queue="exchange", durable=False,
	exclusive=False, auto_delete=True)
chan.exchange_declare(exchange="ex", type="topic", durable=False,
	auto_delete=True,)
chan.queue_bind(queue="exchange", exchange="ex", routing_key="exchange")

event = Event()
		
while True:
	text = json.dumps({ "type": "exchange", "instrument": "ABC", "ask": random.random(), "bid": random.random()})
	msg = amqp.Message(text)
	chan.basic_publish(msg, exchange="ex", routing_key="depth")
	print "published " + text
	event.wait(10)	
