dispatcher.register("market", function() {
	$("body").load("/view/market.html", null, function(res, status, req) {
		mq.subscribe([], function(data) {
			$.each(data, function(i, d) {
				$("body").html($("body").html()	+ "instrument:" + d.instrument + " ask:" + d.ask + " bid:" + d.bid + "<br/>")
			})
		})
	})
})
