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
	def __init__(self, uid, httpdispatcher):
		self.listener = coros.event()
		path = "/client/" + uid

		# listen on http side
		httpdispatcher.register_listener("/comet" + path, self.http_listen)
	
		# listen on client side	
		chan.queue_declare(queue=path, durable=False,exclusive=False, auto_delete=True)
        	chan.queue_bind(queue=path, exchange="ex", routing_key=path)
		chan.basic_consume(queue=path, callback=self.mq_listen, consumer_tag=path)

		print("listening on " + path)

	def mq_listen(msg):
		self.listener.send(msg)
		self.listener.reset()

	def http_listen(req):
		req.write(listener.wait())

client_queues = {}

def login_handler(msg):
	# authenticate
	print msg["user"] + " logged in"

	# assign uid

	if not (msg["user"] in client_queues):	
		# create server side queue for client
		t = "client/" + msg["user"]
		chan.queue_declare(queue=t, durable=False,exclusive=False, auto_delete=True)
		chan.queue_bind(queue="exchange", exchange="ex", routing_key=t)

		# create client side queue for client
		client_queues[msg["user"]] = ClientMessageDispatcher(msg["user"], hd)

	# inform the exchange we got a new client
	mqmsg = amqp.Message(json.dumps({ "type": "subscribe", "user": msg["user"]}))
	chan.basic_publish(mqmsg, exchange="ex", routing_key="exchange")

	return { "uid": msg["user"] } 

md = ServerMessageDispatcher({"login": login_handler})
hd = HTTPDispatcher({"/comet/meta": md.dispatch})
	
# Start the server
httpd.server(api.tcp_listener(('0.0.0.0', 8080)), hd)
