dispatcher.register("ark_ship", function() {
	$("#main").load("/view/ark_ship.html", null, function(res, status, req) {
		$("#universe").click(function() {
			dispatcher.run("universe")
		})
		$("#market").click(function() {
			dispatcher.run("market")
		})

	})
})
