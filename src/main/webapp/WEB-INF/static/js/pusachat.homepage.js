(function($, parent, window, document, undefined) {
	
	var
	
		self = parent.Homepage = parent.Homepage || {},
		
		baseAppUrl = parent['baseAppUrl'],
		api = parent['api'],
		
		body = $("body"),
		isHomepage = !!($("body.homepage").length >= 1),
		
		join = {
			panel: $("div.join"),
			form: $("form.join")
		},
		
		create = {
			panel: $("div.create"),
			form: $("form.create")
		},
		
		input = {
			join: join.form.find("input[type='text']"),
			create: create.form.find("input[type='text']")
		},
		
		roomUrl = create.form.find("p.url"),
		panels = $("div.panel"),
		
		room = {
			getUrl: function(name) {
				return baseAppUrl + "?key=" + name;
			},
			open: function(token) {
				// NOTE: IE does not like it when the name of the window
				// contains a hyphen "-" so the name of the window here
				// is "pusa_" followed by some random string.
				window.open(api + 'room/' + token,
					("pusa_" + Math.floor(Math.random()*16777215).toString(16)),
					"location=1,status=0,scrollbars=0,width=400,height=632");
			},
			random: function() {
				$.ajax({
	    			url: api + 'room/random.json',
	    			dataType: 'json',
	    			success: function(data) {
	    				var name = data['name'];
	    				roomUrl.html(room.getUrl(name));
	    				input.create.val(name);
	    				roomUrl.slideDown("fast");
	    			},
	    			error: function(xhr, status, error) {
	    				alert("Oops, failed to generate random room. Please try again.");
	    			}
	    		});
			},
			create: function(name, clearInput) {
				var token = "";
				$.ajax({
	    			url: api + 'room/create.json',
	    			data: {'name':name},
	    			async: false, // important!
	    			dataType: 'json',
	    			type: 'POST',
	    			success: function(data) {
	    				token = data['token'];
	    			},
	    			error: function(xhr, status, error) {
	    				if(xhr.status == 400) {
	    					// Bad room name.
	    					alert("Oops, please enter a valid room name.");
	    				} else {
	    					// General error occured while joining room.
	    					alert("Oops, failed to create room. Please try again.");
	    				}
	    			}
	    		});
				if(token!=="") {
					room.open(token);
					// Clear the input boxes.
					if(clearInput) {
						input.create.val("");
						input.join.val("");
					}
				}
			},
			join: function(name, clearInput) {
				var token = "";
				$.ajax({
	    			url: api + 'room/join.json',
	    			data: {'name':name},
	    			async: false, // important!
	    			dataType: 'json',
	    			type: 'POST',
	    			success: function(data) {
	    				token = data['token'];
	    			},
	    			error: function(xhr, status, error) {
	    				if(xhr.status == 400) {
	    					// Bad room name.
	    					alert("Oops, please enter a valid room name.");
	    				} else if(xhr.status == 404) {    					
	    					// Room was not found.
	    					if(confirm("This room does not exist.\nWould you like to create it?")) {
	    						room.create(name, clearInput);
	    					}
	    				} else {
	    					// General error occured while joining room.
	    					alert("Oops, failed to join room. Please try again.");
	    				}
	    			}
	    		});
				if(token!=="") {
					room.open(token);
					// Clear the input boxes.
					if(clearInput) {
						input.create.val("");
						input.join.val("");
					}
				}
			}
		},
				
	    init = function() {
			// Join room form handler
			join.form.unbind().bind('submit', function(e) {
				var roomName = input.join.val();
				room.join(roomName, true);
				e.preventDefault();
			});
			input.join.focus(); // Yay!
			// Create new room form handler
			create.form.unbind().bind('submit', function(e) {
				var roomName = input.create.val();
				room.create(roomName, false);
				e.preventDefault();
			});
			// Link handlers too
			join.panel.find("a.create").unbind().click(function(e) {
				panels.hide();
				create.panel.show();
				room.random();
				e.preventDefault();
			});
			create.panel.find("a.join").unbind().click(function(e) {
				panels.hide();
				join.panel.show();
				e.preventDefault();
			});
			create.panel.find("input.random").unbind().click(function(e) {
				room.random();
				e.preventDefault();
			});
			// Key up
			input.create.unbind().bind("keyup", function(e) {
				var name = $(this).val();
				if(name!=="") {
					roomUrl.slideDown("fast");
					roomUrl.html(room.getUrl(name));
				} else {
					roomUrl.slideUp("fast");
				}				
			});
		};
		
	isHomepage && init();
    
})(jQuery, PusaChat || {}, this, this.document);
