dispatcher.register("universe", function() {
	function universe_contents(sector) {
		if(sector != null) {
			var html = new Array()
			$.each(sector, function(i, d) {
				if(d["type"] == "resources") {
					html.push(d["item"])
					html.push(":")
					html.push(d["yield"])
					html.push("<br/>")
				}
				else if(d["type"] == "ships") {
					html.push(d["ship_type"])
					html.push("(")
					html.push(d["owner"])
					html.push(")")	
				}
			})
			return html.join("")
		} else {
			return ""
		}
	}

	function extract_key(key) {
		key = key.substring(1, key.length - 1)
		key = key.split(" ")
		return { x: parseInt(key[0]), y: parseInt(key[1]) }
	}

	$("#main").load("/view/universe.html", null, function(res, status, req) {
		var universe = new Array()
		var shipmeta

		var state = {
			action: select 
		} 
		
		function select(x,y) {
			$("#sector h2").html("Sector " + x + ":" + y) 
			$("#sector div").html(universe_contents(universe[y][x]))

			if(state.x != undefined && state.y != undefined) {
				$("#" + state.x + "_" + state.y).css("background-color", "black")
			}

			$("#" + x + "_" + y).css("background-color", "red")

			$("#ship h2").html("")
			$("#ship div").html("")

			var sector = universe[y][x]
			if(sector != undefined) {
				var ship = sector.filter(function(s) {
					return s.type == "ships" && s.owner == session.user 
				})
				if(ship.length > 0) {
					ship = ship[0]
				
					$("#ship h2").html(ship.ship_type)
					var html = new Array()
					html.push("shields: ")
					html.push(ship.shields)
					html.push("<br/>")
					html.push("<input id=\"move\" type=\"button\" value=\"move\"/>")

					if(ship.ship_type == "ark ship") {
						html.push("<br/>")
						html.push("<input id=\"build\" type=\"button\" value=\"build\"/>")
					}
			
					$("#ship div").html(html.join(""))
					$("#move").click(function() {
						state.selected = ship
						state.action = move
						$(this).attr("value", "hold")
						$(this).click(function() {
							state.action = select 
							$(this).attr("value", "move")	
						})
					})

					if(ship.ship_type == "ark ship") {
						$("#build").click(function() {
							dispatcher.run("build")
						})
					}	
				}
			}
			
			state.x = x
			state.y = y
		}
	
		function move(x,y) {
			if(confirm("move to " + x + " " + y)) {
				mq.send({ type: "universe", action: "move", from: [state.selected.x, state.selected.y], to: [x, y] })
				state.action = select
			}
		}

		$("#market").click(function() {
			mq.unsubscribe("universe")
			dispatcher.run("market")
		})

		mq.subscribe("universe", function(payload) {
			// find highest row/col
			var x_max = 0
			var y_max = 0
			for(var key in payload) {
				key = extract_key(key)
				if(key.x > x_max) x_max = key.x
				if(key.y > y_max) y_max = key.y
			}
		
			// create grid
			for(var y = 0 ; y <= y_max ; y++) {
				universe[y] = new Array(x_max)
			}

			// populate the grid
			for(var key in payload) {
				var t = extract_key(key)
				universe[t.y][t.x] = payload[key]
			}
			
			var html = new Array()
			for(var y = 0 ; y <= y_max ; y++) {
				html.push("<tr>")
				for(var x = 0 ; x <= x_max ; x++) {
					html.push("<td id=\"")
					html.push(x)
					html.push("_")
					html.push(y)	
					html.push("\">")
					html.push(universe_contents(universe[y][x]))
					html.push("</td>")	
				}
				html.push("</tr>")
			}
			$("#universe").html(html.join(""))
			$("#universe").draggable()
			
			// add the click handlers
			for(var y = 0 ; y <= y_max ; y++) {
				for(var x = 0 ; x <= x_max ; x++) {
					$("#" + x + "_" + y).click(function() {
						var xy = $(this).attr("id").split("_")
						state.action(parseInt(xy[0]), parseInt(xy[1]))
					})	
				}
			}

			if(state.x != undefined && state.y != undefined) {
				select(state.x, state.y)
			}
		})

		mq.send({ type: "universe", action: "universe" })
	})
})
