from eventlet import api, httpd, coros, util
from amqplib import client_0_8 as amqp
import simplejson as json
 
util.wrap_socket_with_coroutine_socket()

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
		req.write(self.__mapping[msg["type"]](msg))		

class ClientMessageQueue(object):
	def __init__(self, user, uid, httpdispatcher):
		self.__queue = "client." + user
		conn = amqp.Connection(host="localhost:5672 ", userid="guest", password="guest", virtual_host="/", insist=False)
		self.__chan = conn.channel()
		self.__chan.exchange_declare(exchange="ex", type="topic", durable=False, auto_delete=True)
		self.__chan.queue_declare(queue=self.__queue, durable=False,exclusive=False, auto_delete=True)
        	self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key= "depth")
	
		# listen on the http side	
		httpdispatcher.register_listener("/comet/client/" + uid, self.listen)
		print("registered " + user)

	def listen(self, req):
		msgs = []
		msg = self.__chan.basic_get(queue=self.__queue)
		while msg is None:
			api.sleep()
			msg = self.__chan.basic_get(queue=self.__queue)
		
		while msg is not None:
			msgs.append(msg.body)
			self.__chan.basic_ack(msg.delivery_tag)
			msg = self.__chan.basic_get(queue=self.__queue)
		
		req.write("[" + ",".join(msgs) + "]")

client_queues = {}

def login_handler(msg):
	# authenticate
	print msg["user"] + " logged in"

	# assign uid

	if not (msg["user"] in client_queues):	
		client_queues[msg["user"]] = ClientMessageQueue(msg["user"], msg["user"], hd)

	return json.dumps({ "uid": msg["user"] })

md = ServerMessageDispatcher({"login": login_handler})
hd = HTTPDispatcher({"/comet/meta": md.dispatch})
	
# Start the server
httpd.server(api.tcp_listener(('0.0.0.0', 8080)), hd)
