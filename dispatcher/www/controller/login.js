dispatcher.register("login", function() {
	$("body").load("/view/login.html", null, function(res, status, req) {
		$("#submit").click(function() {
			mq.send({
					type: "login",
					user: $("#username").val(),
					pass: $("#password").val()
				}, function(data, status) {
					if(status == "success") {
						session["uid"] = data.uid
						dispatcher.run("market")
					} else alert("bad username/password")
				})
		})	
	})
})
