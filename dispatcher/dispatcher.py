from eventlet import api, httpd, coros, util
from amqplib import client_0_8 as amqp
import simplejson as json
 
util.wrap_socket_with_coroutine_socket()

conn = amqp.Connection(host="localhost:5672 ", userid="guest", password="guest", virtual_host="/", insist=False)
chan = conn.channel()

class HTTPDispatcher(object):
	def __init__(self, mapping = {}):
		self.__mapping = mapping

	def register_listener(self, path, fn):
		self.__mapping[path] = fn

	def handle_request(self, req):
		self.__mapping[req.path()](req)


class ServerMessageDispatcher(object):
	def __init__(self, mapping = {}):
		self.__mapping = mapping

	def dispatch(self, req):
		msg = json.loads(req.read_body())
		req.write(json.dumps(self.__mapping[msg["type"]](msg)))		

class ClientMessageDispatcher(object):
	def __init__(self, user, uid, httpdispatcher):
		# listen on http side
		httpdispatcher.register_listener("/comet/client/" + uid, self.listen)
	
		# listen on client side
		self.__queue = "/client/" + user
		chan.queue_declare(queue=self.__queue, durable=False,exclusive=False, auto_delete=True)
        	chan.queue_bind(queue=self.__queue, exchange="ex", routing_key=self.__queue)

		print("registered " + user)

	def listen(self, req):
		msgs = []
		while True:
			msg = chan.basic_get(queue=self.__queue)
			if msg is None:
				api.sleep()
			else:
				while True:
					msgs.append(msg.body)
					msg = chan.basic_get(queue=self.__queue)
					if msg is None:
						req.write("[" + ",".join(msgs) + "]")
						return
					else:
						msgs.append(msg.body)
						chan.basic_ack(msg.delivery_tag)
	
client_queues = {}

def login_handler(msg):
	# authenticate
	print msg["user"] + " logged in"

	# assign uid

	if not (msg["user"] in client_queues):	
		client_queues[msg["user"]] = ClientMessageDispatcher(msg["user"], msg["user"], hd)

	# inform the exchange we got a new client
	mqmsg = amqp.Message(json.dumps({ "type": "subscribe", "user": msg["user"]}))
	chan.basic_publish(mqmsg, exchange="ex", routing_key="exchange")

	return { "uid": msg["user"] } 

md = ServerMessageDispatcher({"login": login_handler})
hd = HTTPDispatcher({"/comet/meta": md.dispatch})
	
# Start the server
httpd.server(api.tcp_listener(('0.0.0.0', 8080)), hd)
