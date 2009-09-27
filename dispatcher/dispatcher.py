from eventlet import api, httpd, coros, util
import simplejson as json
 
util.wrap_socket_with_coroutine_socket()

listener = coros.event()
 
class Dispatcher(object):
	def __init__(self, mapping = {}):
		self.__mapping = mapping

	def register_listener(self, path, fn):
		self.__mapping[path] = fn

	def handle_request(self, req):
		self.__mapping[req.path()](req)

def meta_dispatch(req):
	msg = json.loads(req.read_body())
	print msg
	req.write(json.dumps({"uid": msg["user"]}))
			
def message(req):
	req.write("message sent")
	listener.send("hello world")
	listener.reset()

def listen(req):
	req.write(listener.wait())
	
 
# Start the server
httpd.server(
	api.tcp_listener(('0.0.0.0', 8080)),
	Dispatcher({
		"/comet/meta": meta_dispatch,
		"/comet/message": message,
		"/comet/listen": listen}))
