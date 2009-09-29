from eventlet import api
import mq
import time
import simplejson as json
import sqlite3

login_db = sqlite3.connect("client.db")
__client_queues__ = {}

class ClientMessageQueue(object):
        def __init__(self, user):
		self.user = user
                self.__queue = "client." + user
                self.__chan = mq.conn().channel()
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

		# update last
		login_db.execute("update clients set last=? where user=?", (time.time(), self.user))
		login_db.commit()

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
		if not (client in __client_queues__):
			__client_queues__[client] = ClientMessageQueue(msg["user"])

		return req.write(json.dumps({"uid": uid}))
	else:
		req.error(401)

def handle(req):
	if req.path() in __client_queues__:
		__client_queues__[req.path()].listen(req)
	else:
		req.response(401)
		req.write("")
