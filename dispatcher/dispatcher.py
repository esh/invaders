from eventlet import api, httpd, coros, util
import simplejson as json
import mq
import client
import chat
 
util.wrap_socket_with_coroutine_socket()

chan = mq.conn().channel()
chan.exchange_declare(exchange="ex", type="topic", durable=False, auto_delete=True)

class Dispatcher(object):
	def handle_request(self, req):
		if req.path() == "/comet/meta":
			msg = json.loads(req.read_body())
			if msg["type"] == "login":
				client.login(req, msg)
			elif msg["type"] == "chat":
				chat.broadcast(req, msg)
			elif msg["type"] == "world":
				msg["user"] = client.resolve(msg["uid"])
				del msg["uid"]
				chan.basic_publish(mq.msg(msg), exchange="ex", routing_key="world")
				req.write("")	
			else:
				# push it into the right queue
				raise Exception("not yet implemented")

		elif req.path().startswith("/comet/client/"):
			client.handle(req)
		else:
			req.response(401)
			req.write("")

# Start the server
httpd.server(api.tcp_listener(('0.0.0.0', 8080)), Dispatcher())
