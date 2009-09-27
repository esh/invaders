from amqplib import client_0_8 as amqp
from threading import Thread, Event
import simplejson as json
import random

conn = amqp.Connection(host="localhost:5672 ", userid="guest",
	password="guest", virtual_host="/", insist=False)
chan = conn.channel()
chan.queue_declare(queue="exchange", durable=False,
	exclusive=False, auto_delete=True)
chan.exchange_declare(exchange="ex", type="direct", durable=False,
	auto_delete=True,)
chan.queue_bind(queue="exchange", exchange="ex", routing_key="exchange")

class PriceGenerator(Thread):
	def __init__(self, queue):
		Thread.__init__(self)	
		self.__queue = queue
		self.__event = Event()
		
	def run(self):
		while True:
			text = json.dumps({ "type": "exchange", "instrument": "ABC", "ask": random.random(), "bid": random.random()})
			msg = amqp.Message(text)
			chan.basic_publish(msg, exchange="ex", routing_key=self.__queue)
			print "published " + text
			self.__event.wait(10)	

def recv(msg):
	print msg.body
	chan.basic_ack(msg.delivery_tag)
	
	msg = json.loads(msg.body)
	PriceGenerator("/client/" + msg["user"]).start()


chan.basic_consume(queue="exchange", callback=recv, consumer_tag="recv")

while True:
	chan.wait()
