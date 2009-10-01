dispatcher.register("farm", function() {
	$("#main").load("/view/farm.html", null, function(res, status, req) {
		$("#visit_market").click(function() {
			dispatcher.run("market")
		})	
	})
})
