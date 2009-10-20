from eventlet import api, wsgi, coros, util
import simplejson as json
import mq
import client
import chat
 
util.wrap_socket_with_coroutine_socket()

chan = mq.conn().channel()
chan.exchange_declare(exchange="ex", type="topic", durable=False, auto_delete=True)

def dispatch(env, start_response):
	path = env['PATH_INFO']

	if path == "/comet/meta":
		start_response('200 OK', [('Content-Type', 'text/plain')])
		body = env['wsgi.input'].read()
		msg = json.loads(body)
		if msg["type"] == "login":
			return [client.login(msg)]
		elif msg["type"] == "chat":
			chat.broadcast(msg)
		elif msg["type"] == "universe":
			msg["user"] = client.resolve(msg["uid"])
			del msg["uid"]
			chan.basic_publish(mq.msg(msg), exchange="ex", routing_key="universe")
		else:
			# push it into the right queue
			raise Exception("not yet implemented")

		return [""]	
	elif path.startswith("/comet/client/"):
		start_response('200 OK', [('Content-Type', 'text/plain')])
		return [client.handle(env)]
	else:
		return ["401"]

# Start the server
wsgi.server(api.tcp_listener(('0.0.0.0', 8080)), dispatch)
