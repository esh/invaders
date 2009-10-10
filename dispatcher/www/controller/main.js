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
		}) 


		mq.subscribe("snapshot", function(data) {
			$("#chat div").append("<br/>" + data)
		})

		// ask for a snapshot		
		mq.send({ type: "world", action: "snapshot" })
	
		dispatcher.run("farm")
	})
})
