import mq
import simplejson as json
import client

chan = mq.conn().channel()

def broadcast(msg):
	msg["payload"]["user"] = client.resolve(msg["uid"])
	del msg["uid"]
	chan.basic_publish(mq.msg(msg), exchange="ex", routing_key="chat")
