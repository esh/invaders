from eventlet import api
from amqplib import client_0_8 as amqp
import mq
import time
import simplejson as json
import sqlite3

login_db = sqlite3.connect("client.db")
_client_queues = {}

class ClientMessageQueue(object):
        def __init__(self, user):
		self.user = user
		self.last = time.time()
                self.__queue = "client." + user
              	self.__conn = mq.conn() 
		self.__chan = self.__conn.channel()
                self.__chan.exchange_declare(exchange="ex", type="topic", durable=False, auto_delete=True)
                self.__chan.queue_declare(queue=self.__queue, durable=False,exclusive=False, auto_delete=True)
                self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key="depth")
		self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key="chat")
                
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
		self.last = time.time()

	def disconnect(self):
		print "disconecting " + user
		self.__chan.queue_delete(self.__queue, True)
		self.__chan.close()
		self.__conn.close()	
	
def login(req, msg):
        # authenticate
	cur = login_db.execute("select * from clients where user=?", (msg["user"],))
	row = cur.fetchone()
	# hash the password and compare it to the database
	if row is not None:
		print msg["user"] + " logged in"

		# find uid
		uid = msg["user"]

		login_db.execute("update clients set status='online', uid=?, last=? where user=?", (uid, time.time(), msg["user"]))
		login_db.commit()

		# bind to mq if needed
		client = "/comet/client/" + uid
		if not (client in _client_queues):
			_client_queues[client] = ClientMessageQueue(msg["user"])

		return req.write(json.dumps({"uid": uid}))
	else:
		req.error(401)

def handle(req):
	if req.path() in _client_queues:
		_client_queues[req.path()].listen(req)
	else:
		req.response(401)
		req.write("")

def _check_timeout():
	now = time.time()
	for client in _client_queues:
		# if longer then 10 minutes
		if now - _client_queues[client].last > 1000 * 60 * 10:
			# once this is distributed, need to check db to see if we should disconnect client
			login_db.execute("update clients set status='offline', last=? where user=?", (time.time(), _client_queues[client].user))
			login_db.commit()

			del _client_queues[client]
		else:
			login_db.execute("update clients set last=? where user=?", (time.time(), _client_queues[client].user))
			login_db.commit()

	api.call_after(60, _check_timeout)

_check_timeout()
