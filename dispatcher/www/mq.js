var mq = (function() {
	var subscriptions = new Object()
	var uid 

	return {
		poll: function(_uid) {
			uid = _uid

			function _poll() {
				// send a msg on meta describing the subscriptions
				$.ajax({
					type: "GET",
					url: "/comet/client/" + uid,
					async: true,
					cache: false,
					dataType: "json",
					success: function(data) {
						$.each(data, function(i, d) {
							var fn = subscriptions[d.type]
							if(fn != undefined && fn != null) {
								fn(d)
							}
						})
				
						setTimeout(_poll, 50)	
					},
					error: function(XMLHttpRequest, status, error) {
						alert(error)
						setTimeout(_poll, 50)	
					}})
			}
			_poll()
		},
		send: function(msg, fn) {
			if(uid != undefined) msg["uid"] = uid
			$.post("/comet/meta", $.toJSON(msg), fn, "json") 
		},
		subscribe: function(topic, fn) {
			subscriptions[topic] = fn
		},
		unsubscribe: function(topic) {
			delete subscriptions[topic]
		}
	}
})()
