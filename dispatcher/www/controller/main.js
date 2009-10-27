dispatcher.register("main", function() {
	$("body").load("/view/main.html", null, function(res, status, req) {
		// setup chat
		$("#chat input").keyup(function(e) {
			if(e.keyCode==13) {
				mq.send({
					type: "chat",
					text: $("#chat input").val()
				})
				$("#chat input").val("")
			} 
		})
		mq.subscribe("chat", function(data) {
			$("#chat div").append("<br/>" + data["user"] + "> " + data["text"])
			$("#chat div").scrollTo("max")
		}) 

		mq.subscribe("possessions", function(data) {
			for(var key in data) {
				if(key != "type") $("#status").append(key + ": " + data[key] + "&nbsp;")
			}
		})

		mq.send({ type: "universe", action: "possessions" })
	
		dispatcher.run("universe")
	})
})
