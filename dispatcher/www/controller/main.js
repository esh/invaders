dispatcher.register("main", function() {
	$("body").load("/view/main.html", null, function(res, status, req) {
		// setup chat
		$("#chat input").keyup(function(e) {
			if(e.keyCode==13) {
				mq.send({
					type: "chat",
					payload: {
						text: $("#chat input").val()
					}
				})
				$("#chat input").val("")
			} 
		})
		mq.subscribe("chat", function(payload) {
			$("#chat div").append("<br/>" + payload.user + "> " + payload.text)
			$("#chat div").scrollTo("max")
		}) 

		mq.subscribe("possessions", function(payload) {
			var html = new Array()
			for(var key in payload) {
				html.push(key + ": " + payload[key] + "&nbsp;")
			}
			$("#status").html(html.join(""))
		})

		mq.send({ type: "universe", action: "possessions" })
	
		dispatcher.run("universe")
	})
})
