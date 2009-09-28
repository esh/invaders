dispatcher.register("market", function() {
	$("body").load("/view/market.html", null, function(res, status, req) {
		mq.subscribe("exchange", function(data) {
			$("body").html($("body").html()	+ "instrument:" + data.instrument + " ask:" + data.ask + " bid:" + data.bid + "<br/>")
		})
	})
})
