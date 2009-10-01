dispatcher.register("market", function() {
	$("#main").load("/view/market.html", null, function(res, status, req) {
		mq.subscribe("exchange", function(data) {
			$("#pane").append("instrument:" + data.instrument + " ask:" + data.ask + " bid:" + data.bid + "<br/>")
		})

		$("#back_to_farm").click(function() {
			mq.unsubscribe("exchange")
			dispatcher.run("farm")
		})
	})
})
