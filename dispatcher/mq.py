from amqplib import client_0_8 as amqp
import simplejson as json

def conn():
	return amqp.Connection(host="localhost:5672 ", userid="guest", password="guest", virtual_host="/",insist=False)

def msg(json_object): 
	return amqp.Message(json.dumps(json_object))
