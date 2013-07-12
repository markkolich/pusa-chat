var PusaChat = (function($, parent, document, undefined) {
	
	var
	
		// Namespace.
		self = {},
		
		// The documents default location and associated hostname.
		location = parent.location,
		hostname = location.hostname,
		port = location.port,
		protocol = location.protocol,
		
		// Production domains, and port (if any)
		domains = ['onyx.koli.ch', 'pusa'],
		
		// If the detected hostname is not a production domain, then we
		// must be in development.  In which case, the path below
		// will not be '/' but rather something behind Tomcat.
		debug = !!($.inArray(hostname, domains) == -1),
		path = (debug ? '/' : '/pusa-chat/'),
		api = path + 'api/',
		
		// Dynamically build the base app URL.
		baseAppUrl = protocol + '//' + hostname + ((port!=='') ? ':' + port : '') + path,
	    
	    // Verify if we have the HTML5 History API in this browser.  Also
	    // validate that the browser supports window.onhashchange.  NOTE: The
	    // !!(expr) you see is to ensure that the inner expression evaluates
	    // to a hard boolean, and not a String representation of 'true' or
	    // 'false'.
	    //pushStateExists = !!(parent.history && parent.history.pushState), 
	    //hashChangeExists = !!(parent.onhashchange),
	    
	    // Do we have access to window.console as provided by Firebug?
	    // If not, leave things undefined in which case 
	    console = !!(parent.console) ? parent.console : undefined,
	    		
	    init = (function() {
	    	return function() {
	    		// If no console exists, then we define a stub console for
	    		// each supported Firebug logging function.
	    		if(!console) {
	    			var names = ["log", "debug", "info", "warn", "error",
	    			    "assert", "dir", "dirxml", "group", "groupEnd","time",
	    			    "timeEnd", "count", "trace", "profile", "profileEnd"];
	    			console = {};
    				for(var i=0, l=names.length; i<l; i++) {
    					console[names[i]] = function(){};
    				}
	    		}
	    	};
	    }());
		
	self['path'] = path;
	self['api'] = api;
	self['baseAppUrl'] = baseAppUrl;
	
	self['debug'] = debug;
	self['console'] = console;
	
	init();
	
	return self;
    
})(jQuery, this, this.document);
