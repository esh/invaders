var mq = (function() {
	var subscriptions = new Object()
	
	return {
		send: function(msg, fn) {
			$.post("/comet/meta", $.toJSON(msg), fn, "json") 
		},
		subscribe: function(type, fn) {

		}
	}
})()
