dispatcher.register("login", function() {
	$("body").load("/view/login.html", null, function(res, status, req) {
		$("#submit").click(function() {
			mq.send({
					type: "login",
					user: $("#username").val(),
					pass: $("#password").val()
				}, function(data, status) {
					if(status == "success") {
						$("body").load("/view/main.html", null, function(res, status, req) {
							mq.poll(data.uid)

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
					} else alert("bad username/password")
				})
		})	
	})
})
