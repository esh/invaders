var mq = (function() {
	return {
		send: function(msg, fn) {
			$.post("/comet/meta", $.toJSON(msg), fn, "json") 
		},
		subscribe: function(types, fn) {
			function sub() {
				// send a msg on meta describing the subscriptions
				$.ajax({
					type: "GET",
					url: "/comet/client/" + session.uid,
					dataType: "json",
					success: function(data) {
						fn(data)
						sub()
					},
					error: function(XMLHttpRequest, status, error) {
						sub()
					}})
			}

			sub()
		}
	}
})()
