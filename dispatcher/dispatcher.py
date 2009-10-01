from eventlet import api, httpd, coros, util
import simplejson as json
import client
import chat
 
util.wrap_socket_with_coroutine_socket()

class Dispatcher(object):
	def handle_request(self, req):
		if req.path() == "/comet/meta":
			msg = json.loads(req.read_body())
			if msg["type"] == "login":
				client.login(req, msg)
			elif msg["type"] == "chat":
				chat.broadcast(req, msg)	
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
