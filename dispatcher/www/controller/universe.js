dispatcher.register("universe", function() {
	$("#main").load("/view/universe.html", null, function(res, status, req) {
		$("#market").click(function() {
			dispatcher.run("market")
		})
		$("#ark_ship").click(function() {
			dispatcher.run("ark_ship")
		})

		mq.subscribe("universe", function(data) {
			for(var key in data) {
				if(key != "type") $("#pane").append(key + ": " + data[key] + "&nbsp;")
			}
		})

		mq.send({ type: "universe", action: "universe" })
	})
})
