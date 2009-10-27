dispatcher.register("universe", function() {
	$("#main").load("/view/universe.html", null, function(res, status, req) {
		var universe = new Array()

		$("#market").click(function() {
			mq.unsubscribe("universe")
			dispatcher.run("market")
		})
		$("#ark_ship").click(function() {
			mq.unsubscribe("universe")
			dispatcher.run("ark_ship")
		})

		mq.subscribe("universe", function(data) {
			function extract_key(key) {
				key = key.substring(1, key.length - 1)
				key = key.split(" ")
				return { x: parseInt(key[0]), y: parseInt(key[1]) }
			}

			delete data["type"]
			// find highest row/col
			var x_max = 0
			var y_max = 0
			for(var key in data) {
				key = extract_key(key)
				if(key.x > x_max) x_max = key.x
				if(key.y > y_max) y_max = key.y
			}
		
			// create grid
			for(var y = 0 ; y <= y_max ; y++) {
				universe[y] = new Array(x_max)
			}

			// populate the grid
			for(var key in data) {
				var t = extract_key(key)
				universe[t.y][t.x] = data[key]
			}
			
			var html = new Array()
			for(var y = 0 ; y <= y_max ; y++) {
				html.push("<tr>")
				for(var x = 0 ; x <= x_max ; x++) {
					html.push("<td>")
					if(universe[y][x] != null) {
						$.each(universe[y][x], function(i, d) {
							if(d["type"] == "resources") {
								html.push(d["item"])
								html.push(":")
								html.push(d["yield"])
								html.push("<br/>")
							}
						})
					}
					html.push("</td>")	
				}
				html.push("</tr>")
			}
			$("#universe").html(html.join(""))	
		})

		mq.send({ type: "universe", action: "universe" })
	})
})
