dispatcher.register("main", function() {
	$("body").load("/view/main.html", null, function(res, status, req) {
		// setup chat
		$("#chat_input").keyup(function(e) {
			if(e.keyCode==13) {
				mq.send({
					type: "chat",
					text: $("#chat_input").val()
				})
				$("#chat_input").val("")
			} 
		})
		mq.subscribe("chat", function(data) {
			$("#chat_area").html($("#chat_area").html() + "<br/>" + data["text"])
		}) 

		dispatcher.run("market")
	})
})
