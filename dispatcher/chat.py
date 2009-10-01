import mq
import simplejson as json
import client

chan = mq.conn().channel()
chan.exchange_declare(exchange="ex", type="topic", durable=False, auto_delete=True)

def broadcast(req, msg):
	msg["user"] = client.resolve(msg["uid"])
	del msg["uid"]
	chan.basic_publish(mq.msg(msg), exchange="ex", routing_key="chat")
	
	req.write("")
