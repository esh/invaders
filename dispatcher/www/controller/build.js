dispatcher.register("build", function() {
	$("#main").load("/view/build.html", null, function(res, status, req) {
		$("#back").click(function() {
			mq.unsubscribe("ships-meta")
			dispatcher.run("universe")
		})	

		mq.subscribe("ship-meta", function(payload) {
			shipmeta = payload
			var html = new Array()
			for(var ship in shipmeta) {
				var costs = shipmeta[ship].cost
				var id = 0	
				if(costs) {
					html.push(ship)
					for(var i = 0 ; i < costs.length ; i++) {
						html.push(" ")
						html.push(costs[i].item)
						html.push(":")
						html.push(costs[i].qty)
					}
					html.push("<input type=\"button\"")
					html.push("\" value=\"build\" onClick=\"")
					html.push("create_ship('")
					html.push(ship)
					html.push("')\"/>")
					html.push("<br/>")
					id++
				}
			}
			$("#build_menu").html(html.join(""))	
		})

		mq.send({ type: "universe", action: "ship-meta" })
	})
})
