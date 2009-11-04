from eventlet import api
from amqplib import client_0_8 as amqp
import mq
import time
import simplejson as json
import sqlite3

login_db = sqlite3.connect("../db/clients.db")
_client_queues = {}

class ClientMessageQueue(object):
        def __init__(self, user):
		self.user = user
		self.last = time.time()
                self.__queue = "client." + user
              	self.__conn = mq.conn() 
		self.__chan = self.__conn.channel()
                self.__chan.queue_declare(queue=self.__queue, durable=False,exclusive=False, auto_delete=True)
		self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key="client." + user)
                self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key="depth")
		self.__chan.queue_bind(queue=self.__queue, exchange="ex", routing_key="chat")
                
		print("registered " + user)

        def listen(self):
		last = time.time()
		self.last = last 

                msgs = []
		while last == self.last:
                	msg = self.__chan.basic_get(queue=self.__queue)
			if msg is not None:
	                        msgs.append(msg.body)
				self.__chan.basic_ack(msg.delivery_tag)
			elif msg is None and len(msgs) > 0:
                		return "[" + ",".join(msgs) + "]"
			else:
				api.sleep(0.15)

		return "timeout"

	def disconnect(self):
		print "disconecting " + user
		self.__chan.queue_delete(self.__queue, True)
		self.__chan.close()
		self.__conn.close()	

def init():
	cur = login_db.execute("select * from clients")
	for row in cur:
		if row[3] == "online":
			client = "/comet/client/" + row[1] 
			_client_queues[client] = ClientMessageQueue(row[0])
			_client_queues[client].last = row[2] 

	api.spawn(_check_timeout)

def login(msg):
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

		return json.dumps({"uid": uid})
	else:
		raise Exception("unauthorized")

def handle(env):
	if env['PATH_INFO'] in _client_queues:
		return _client_queues[env['PATH_INFO']].listen()
	else:
		raise Exception("unauthorized")		

def resolve(uid):
	client = "/comet/client/" + uid
	if client not in _client_queues:
		raise Exception("client does not exist")
	else:
		return _client_queues[client].user

def _check_timeout():
	while True:
		now = time.time()
		for client in _client_queues.keys():
			if now - _client_queues[client].last > 60 * 8:
				login_db.execute("update clients set status='offline', last=? where user=?", (time.time(), _client_queues[client].user))
				login_db.commit()

				del _client_queues[client]
			else:
				login_db.execute("update clients set last=? where user=?", (_client_queues[client].last, _client_queues[client].user))
				login_db.commit()
		api.sleep(5)
