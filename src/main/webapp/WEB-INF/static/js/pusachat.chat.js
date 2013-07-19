(function($, parent, window, document, undefined) {
	
	var
	
		// Namespace.
		self = parent.Chat = parent.Chat || {},
		
		baseAppUrl = parent['baseAppUrl'],
		api = parent['api'],
				
		timeouts = {
			NONE: 0, // zero
			NORMAL: 100, // 100 ms
			MESSAGE: 3000, // 3 sec
			ERROR: 5000, // 5 sec
			INACTIVITY: 600000 // 10 min
		},
				
		room = {
			// The room "id" is actually a digitally signed UUID
			// that represents the room internally to the web-application.
			// The actual UUID of the room is ~never~ exposed to the public.
			id: $("body").attr("id"),
			token: null,
			clientId: null,
			maxMessages: 100, // DEFAULT
			timeStampFormat: "h:mm:ss a",
			userStatus: $("p.status"),
			typingStatus: $("p.typing"),
			chat: {
				container: $("div.chat"),
				messageBox: $("#message-box"),
				inputBox: $("#message-box").find("input[type='text']")
			}
		},
		
		errors = {
			modals: {
				registration: $("div.error.registration"),
				unrecoverable: $("div.error.unrecoverable"),
				temporary: $("div.error.temporary")
			}
		},
		
		// Logic borrowed from http://williamsportwebdeveloper.com/cgi/wp/?p=503
		// Refactored for my own needs.
		toISO8601String = (function() {
			var padzero = function(n) {
				return n < 10 ? '0' + n : n;
			},
			pad2zeros = function(n) {
				if (n < 100) {
					n = '0' + n;
				}
				if (n < 10) {
					n = '0' + n;
				}
				return n;
			};
	    	return function(d) {
	    		return d.getUTCFullYear() + '-'
	    			+ padzero(d.getUTCMonth() + 1)
	    			+ '-' + padzero(d.getUTCDate())
	    			+ 'T'
	    			+ padzero(d.getUTCHours()) + ':'
	    			+ padzero(d.getUTCMinutes()) + ':'
	    			+ padzero(d.getUTCSeconds()) + '.'
	    			+ pad2zeros(d.getUTCMilliseconds())
	    			+ 'Z';
	    	};
	    }()),
		
		chat = {
			colorSessionMap: [],
			getRandomColor: function() {
				return ("#" + Math.floor(Math.random()*16777215).toString(16));
			},
			getColorForClient: function(clientId) {
				if(chat.colorSessionMap[clientId] == undefined) {
					chat.colorSessionMap[clientId] = chat.getRandomColor();
				}
				return chat.colorSessionMap[clientId];
			},
			resize: function() {
				var lis = room.chat.container.find("li");
				if(lis.length > room.maxMessages) {
					// Remove the first <li> if we're over the
					// max message limit.
					room.chat.container.find("li:lt(1)").remove();
				}
				room.chat.container.scrollTop(room.chat.container[0].scrollHeight);
			},
			showMessage: function(message) {
				var id = message['id'], when = message['when'], html = message['html'], from = message['from'];
				if(room.chat.container.find("#"+id).length <= 0) {
					var ul = room.chat.container.find("ul");
					var li = $("<li/>").attr("id", id), msgWrapper = $("<div/>").addClass("message"), msgText = $("<p/>").html(html), timestamp = $("<p/>").addClass("timestamp");
					var deleteImg = $("<img/>").attr("src",baseAppUrl+"images/delete.gif"), deleteIcon = $("<p/>").addClass("delete").append(deleteImg);
					// Set the timestamp on the message.
					timestamp.html($.localtime.toLocalTime(when, room.timeStampFormat));
					msgWrapper.append(msgText).append(timestamp);
					if(from.indexOf("error-") == 0) {
						li.addClass("error");
					} else {
						var clientColor = chat.getColorForClient(from);
						if(from !== room.clientId) {
							li.addClass("others").css("border-color", clientColor);
							msgWrapper.addClass("others").css("border-color", clientColor);					
						} else if(from === room.clientId) {
							li.addClass("me");
							// Can only delete messages from yourself.
							deleteIcon.unbind().click(function(e){
								var li = $(e.target).parents("li[id]:first");
								(li != undefined) && bosh.deleteMessage(li);
							}).appendTo(msgWrapper);				
						}						
					}
					li.unbind().hover(function(e) {
						// Hide all others, then only show the one we
						// care about.
						room.chat.container.find("p.delete").hide();		
						$(e.target).find("p.delete").show();
					}, function(e) {
						// Hide all.
						room.chat.container.find("p.delete").hide();
					});
					li.append(msgWrapper);
					ul.append(li);
					chat.resize();
				}
			},
			showError: function(message) {
				var errorId = "error-" + chat.getRandomColor();
				chat.showMessage({
					'html': message,
					'id': errorId,
					'from': errorId,
					// Convert the current Date into a valid ISO-8601
					// formatted date string.
					'when': toISO8601String(new Date())
				});
			},
			deleteMessage: function(id) {
				room.chat.container.find("#"+id).remove();
			},
			userStatus: function(userCount, inactiveUserCount) {
				var status = userCount + " " + ((userCount==1) ? "user" : "users");
				if(inactiveUserCount > 0) {
					status += ", " + inactiveUserCount + " inactive";
				}
				room.userStatus.html(status);
			},
			typingStatus: function(typing) {
				var status = "", me = 0, others = 0;
				for(var i = 0, l = typing.length; i<l; i++) {
					var t = typing[i];
					if(t == room.clientId) {
						me++;
					} else {
						others++;
					}
				}
				if((me+others) > 0) {
					if(me > 0) {
						status += ("You" + ((others > 0) ? ", and " : " are "));
					}
					if(others > 0) {
						status += (others + ((others == 1) ? " other is " : " others are "));
					}
					status += "typing...";
				} else {
					status = "&nbsp;";
				}
				room.typingStatus.html(status);
			}
		},
		
		bosh = {
			register: function() {
				$.ajax({
	    			url: api + 'chat/register/' + room.id + '.json',
	    			dataType: 'json',
	    			type: 'POST',
	    			async: false, // important!
	    			success: function(data) {
	    				// The token is the BOSH token we can use to uniquely
	    				// identify ourselves in the room.  It is to be used
	    				// when sending requests to the API.  It's a
	    				room.token = data['token'];
	    				// We're assigned a client ID by the server.
	    				room.clientId = data['client_id'];
	    				// Load the chat log.
	    				bosh.fetchLog();
	    				// Initiate the session.
	    				bosh.event();
	    			},
	    			error: function(xhr, ts, errorThrown) {
	    				// If the client failed registration, nothing we can
	    				// do, this chat is useless so bail immeaditely.
	    				errors.modals.registration.modal({'escClose': false});
	    			}
	    		});
			},
			typing: function(typing) {
				$.ajax({
	    			url: api + 'chat/typing/' + room.token + '.json',
	    			data: {'typing': typing},
	    			dataType: 'json',
	    			type: 'POST'
	    		});
			},
			inactivity: function(active) {
				$.ajax({
	    			url: api + 'chat/inactivity/' + room.token + '.json',
	    			data: {'active': active},
	    			dataType: 'json',
	    			type: 'POST'
	    		});
			},			
			sendMessage: function(message) {
				$.ajax({
	    			url: api + 'chat/message/' + room.token + '.json',
	    			data: {'message': message},
	    			dataType: 'json',
	    			type: 'POST',
	    			timeout: timeouts.MESSAGE,
	    			success: function(data) {
	    				// Clear the input box.
	    				room.chat.inputBox.val("").focus();
	    				// Show the message in the chat container immeaditely.
	    				// It's OK if we add this message now, the JavaScript
	    				// will check to see if the message with this ID was
	    				// already added before attempting to add it again
	    				// (so the user won't see duplicate messages).
	    				chat.showMessage(data);
	    			},
	    			error: function(xhr, ts, errorThrown) {
	    				var status = xhr['status'];
	    				chat.showError("Failed to send message; please try again " +
							"(msg=" + ts + ", e=" + errorThrown + ", status=" +
								status + ")");
	    			},
	    			complete: function(xhr, ts) {
	    				// Ugly syntax to work around the fact that we need
	    				// to call $.titleAlert._focus() but we don't want
	    				// Google's closure compiler to obfuscate the function
	    				// name here (so we wrap it in single quotes).
	    				$.titleAlert['_focus']();
	    			}
	    		});
			},
			deleteMessage: function(li) {
				var id = li.attr("id");
				$.ajax({
	    			url: api + 'chat/message/' + room.token + '.json',
	    			data: {'messageId': id},
	    			dataType: 'json',
	    			type: 'DELETE',
	    			timeout: timeouts.MESSAGE,
	    			success: function(data) {
	    				li.remove();
	    			},
	    			error: function(xhr, ts, errorThrown) {
	    				var status = xhr['status'];
	    				chat.showError("Failed to delete message; please try again " +
							"(msg=" + ts + ", e=" + errorThrown + ", status=" +
								status + ")");
	    			}
	    		});
			},
			fetchLog: function() {
				$.ajax({
	    			url: api + 'chat/log/' + room.token + '.json',
	    			dataType: 'json',
	    			type: 'GET',
	    			timeout: timeouts.MESSAGE,
	    			success: function(data) {
	    				var log = data["log"];
	    				for(var i = 0, l = log.length; i < l; i++) {
	    					// Pass the message from the "log" to the
	    					// message renderer to render the message in
	    					// the chat container accordingly.
	    					chat.showMessage(log[i]);
	    				}
	    			}
	    		});
			},
			event: function() {
				var refetch = timeouts.NONE;
				$.ajax({
	    			url: api + 'chat/event/' + room.token + '.json',
	    			dataType: 'json',
	    			success: function(event) {
	    				if(event !== null && (typeof event['type'] !== undefined)) {
	    					var type = event['type'];
	    					if(type == "MESSAGE") {
								chat.showMessage(event);
								var newMessages = (event['from'] !== room.clientId);
								if(newMessages) {
				    				$.titleAlert("[New Messages]", {'requireBlur':true, 'stopOnFocus':true});
				    			}
							} else if(type == "CLIENT_STATUS") {
								chat.userStatus(event['active'], event['inactive']);
							} else if(type == "CLIENT_JOINED") {
								// Ignore the "client joined room" event if the client
								// that joined is "me" (so users don't see a client
								// joined room alert when they are the user that joined).
								var itsMe = (event['client_id'] === room.clientId);
								if(!itsMe) {
									// The user that joined the room is _not_ me
									// must be another user, show the title alert
									// accordingly.
									$.titleAlert("[User Joined Room]", {'requireBlur':true, 'stopOnFocus':true});
								}								
							} else if(type == "TYPING") {
								chat.typingStatus(event['typing']);
							} else if(type == "DELETE") {
								chat.deleteMessage(event['delete']);
							}
							// Any other (unknown?) event type is ignored entirely.
		    				refetch = timeouts.NORMAL;
	    				} else {
	    					refetch = timeouts.ERROR;
	    				}
	    			},
	    			error: function(xhr, ts, errorThrown) {
						var tryAgain = true, status = xhr['status'];
						// Based on the error type, we handled things a
						// bit differently.
						if(ts=="timeout") {
							chat.showError("Connection to server timed out " +
								"(msg=" + ts + ", e=" + errorThrown + ", status=" +
									status + ")");
						} else if(ts=="error") {
							if(status == 503 || status == 404) {
								// Chat server went away.  Or, chat server
								// restarted and this room no longer exists.
								chat.showError("Unrecoverable error occurred while communicating with server " +
									"(msg=" + ts + ", e=" + errorThrown + ", status=" +
										status + ")");
								errors.modals.unrecoverable.modal({'escClose':false});
								// Stop BOSH, things failed enough to give up.
								tryAgain = false;
							} else if(status == 412) {
								// Happens if the chat loses its connection with the
								// server and the "inactive user cleaner" deletes the
								// user from the room "prematurely".  In this case, we
								// should be able to just reload the room and continue.
								bosh.register();
								tryAgain = false;
							} else {
								chat.showError("Error occurred while communicating with server " +
									"(msg=" + ts + ", e=" + errorThrown + ", status=" +
										status + ")");
							}
						} else if(ts=="abort") {
							chat.showError("Connection to server was aborted " +
								"(msg=" + ts + ", e=" + errorThrown + ", status=" +
									status + ")");
						} else {
							chat.showError("Error occurred while communicating with server " +
								"(msg=" + ts + ", e=" + errorThrown + ", status=" +
									status + ")");
						}
						// Try to refetch again only if we're in a mode that allows it.
						refetch = (tryAgain) ? timeouts.ERROR : timeouts.NONE;
	    			},
	    			complete: function() {
	    				if(refetch > timeouts.NONE) {
	    					// Kick off another BOSH (long poll) transaction
	    					// since the refetch was greater than "NONE"
	    					// meaning, in theory, no error occurred and we
	    					// can keep going.
	    					$.after(refetch, function(){
	    						bosh.event();
	    					});
	    				} else {
	    					// Hm, looks like an error occurred and we will not
	    					// kick off another BOSH transaction.  Bail, die.
	    					chat.showError("Error occurred while communicating with server " +
								"(msg=" + ts + ", e=" + errorThrown + ", status=" +
									status + ")");
	    				}
	    			}
		    	});
			}
		},
		
		inactivity = (function() {
			var timer = null, timerSet = false;
	    	return {
	    		set: function() {
					if(!timerSet) {
						timerSet = true;
						// After X-milliseconds, check if the user is still there.
						timer = $.after(timeouts.INACTIVITY, function() {
							timerSet = false;
							bosh.inactivity(false);
						});
					}
				},
				reset: function() {
					if(timerSet) {
						clearTimeout(timer);
						timerSet = false;
					}
					inactivity.set(); // Needs to be inactivity.set(), not just set()
				}
	    	};
	    }()),
	    
	    init = function() {
			// Register this client synchronously, then kick off the BOSH
			// session and away we go!
			bosh.register();
			// Bind the form handler.
			room.chat.messageBox.unbind().bind("submit", function(e) {
				var message = '';
				if((message = room.chat.inputBox.val()) !== '') {
					bosh.sendMessage(message);
				}
				e.preventDefault();
			});
			// For the on key press typing handler.
			room.chat.inputBox.typing({
				start: function(event, $elem) {
					bosh.typing(true); // Typing!
					bosh.inactivity(true); // Is active!
				},
			    stop: function(event, $elem) {
			    	bosh.typing(false); // Stopped typing
			    	inactivity.reset(); // Reset timer
			    },
			    delay: 400
		    });
			// For the inactivity timeout.
			$(window).bind("focus", function() {
				bosh.inactivity(true);
				inactivity.reset();
			});
			room.chat.inputBox.focus();
	    };
	    
	($("body.room").length >= 1) && init();
    
})(jQuery, PusaChat || {}, this, this.document);
