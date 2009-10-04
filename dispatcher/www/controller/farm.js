dispatcher.register("farm", function() {
	$("#main").load("/view/farm.html", null, function(res, status, req) {
		$("#visit_market").click(function() {
			dispatcher.run("market")
		})

		mq.send({ type: "world", action: "snapshot" }, function() { alert("done")})	
	})
})
