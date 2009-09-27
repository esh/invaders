var mq = (function() {
	var subscriptions = new Object()

	return {
		send: function(msg, fn) {
			$.post("/comet/meta", $.toJSON(msg), fn, "json") 
		},
		subscribe: function(types, fn) {
			function sub() {
				// send a msg on meta describing the subscriptions

				$.getJSON("/comet/client/" + session.uid, null, 
					function(data) {
						fn(data)
					sub()	
				})
			/*	
				$.ajaxError(function(event, req, options, error) {
					sub()
				})*/
			}

			sub()
		}
	}
})()
