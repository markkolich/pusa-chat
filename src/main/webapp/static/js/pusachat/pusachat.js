// Setup the global namespace, if not already set.
this['PusaChat'] = this['PusaChat'] || {};

(function($, parent, window, document) {
    'use strict';

    var

        // Namespace.
        self = parent,

        // The documents default location and associated hostname.
        location = window.location,
        hostname = location.hostname,
        port = location.port,
        protocol = location.protocol,

        // The path the app is served under (e.g., "/pusa-chat"), set by the server
        // on the page's <body>.
        contextPath = $('body').attr('data-context-path') || '',
        path = contextPath + '/',
        api = path + 'api/',

        // Dynamically build the base app URL.
        baseAppUrl = protocol + '//' + hostname + ((port !== '') ? ':' + port : '') + path,

        // Do we have access to window.console as provided by Firebug?
        // If not, leave things undefined in which case
        console = window.console ? window.console : undefined,

        init = (function() {
            return function() {
                // If no console exists, then we define a stub console for
                // each supported Firebug logging function.
                if (!console) {
                    var names = ['log', 'debug', 'info', 'warn', 'error',
                        'assert', 'dir', 'dirxml', 'group', 'groupEnd', 'time',
                        'timeEnd', 'count', 'trace', 'profile', 'profileEnd'];
                    console = {};
                    for (var i = 0, l = names.length; i < l; i++) {
                        console[names[i]] = function() {};
                    }
                }
            };
        }());

    init();

    self.path = path;
    self.api = api;
    self.baseAppUrl = baseAppUrl;

    self.console = console;

})(jQuery, PusaChat, this, this.document);
