var dispatcher = (function() {
	var controllers = new Object()
	
	return {
		run: function(controller, params) {
			if(controllers[controller] == undefined) {
				$.getScript("/controller/" + controller + ".js", function() {
					controllers[controller](params)
				})				
			} else controllers[controller].run(params)
		},
		register: function(controller, fn) {
			controllers[controller] = fn 
		}
	}
})()
