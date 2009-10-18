dispatcher.register("universe", function() {
	$("#main").load("/view/universe.html", null, function(res, status, req) {
		$("#visit_market").click(function() {
			dispatcher.run("market")
		})
	})
})
