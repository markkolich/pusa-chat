/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package pusachat;

import com.google.common.io.Resources;
import com.typesafe.config.ConfigFactory;
import curacao.servlet.jakarta.CuracaoJakartaContextListener;
import curacao.servlet.jakarta.CuracaoJakartaDispatcherServlet;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import pusachat.components.config.PusaChatConfig;

import java.net.URL;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Entry point. Boots a Jetty web-server hosting the Curacao dispatcher (the pages and the
 * chat API) and static assets; Curacao in turn instantiates every component under the
 * {@code pusachat} boot-package.
 */
public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String STATIC_SERVLET_MAPPING_UNDER_CONTEXT = "/static/*";
    private static final String FAVICON_SERVLET_MAPPING_UNDER_CONTEXT = "/favicon.ico";
    private static final String ROBOTS_SERVLET_MAPPING_UNDER_CONTEXT = "/robots.txt";
    private static final String CURACAO_SERVLET_MAPPING_UNDER_CONTEXT = "/*";

    public static final String CONTEXT_ATTRIBUTE_BASE_RESOURCE = "pusachat.base-resource";

    @Option(names = {"--host"}, paramLabel = "HOST", description = "HTTP host.",
            defaultValue = "127.0.0.1")
    private String host_;

    @Option(names = {"--port"}, paramLabel = "PORT", description = "HTTP port.",
            defaultValue = "8080", required = true)
    private int port_;

    public static void main(
            final String... args) {
        try {
            final Application app = new Application();
            new CommandLine(app).parseArgs(args);

            final BuildVersion buildVersion = BuildVersion.getInstance();
            LOG.info("Pusa Chat-{}; built: {}; git: {}; jvm {}",
                    buildVersion.getVersion(),
                    buildVersion.getTimestamp(),
                    buildVersion.getBuildNumber(),
                    System.getProperty("java.runtime.version", System.getProperty("java.version")));

            final Server server = buildServer(app.host_, app.port_);
            server.start();
            server.join();
        } catch (final Exception e) {
            LOG.error("Pusa Chat startup failed.", e);
        }
    }

    /**
     * Builds, but does not start, the Jetty server. Package-private so tests can boot the
     * entire stack on an ephemeral port (port 0).
     */
    /* package */ static Server buildServer(
            final String host,
            final int port) throws Exception {
        // Jetty needs the context path before Curacao (and our config component) exist.
        final String contextPath = ConfigFactory.load()
                .getString(PusaChatConfig.PUSACHAT_CONFIG_PATH + "." + PusaChatConfig.CONTEXT_PATH_PROP);

        // Setup a new queued thread pool.
        final QueuedThreadPool pool = new QueuedThreadPool();
        pool.setName("jetty-pusachat");

        // Instantiate a new server instance using said thread pool.
        final Server server = new Server(pool);
        final HttpConfiguration config = new HttpConfiguration();
        config.setSendXPoweredBy(false); // Hide X-Powered-By: Jetty
        config.setSendServerVersion(false); // Hide Server: Jetty-*

        // Grab a NIO connector for the server.
        final HttpConnectionFactory factory = new HttpConnectionFactory(config);
        final ServerConnector connector = new ServerConnector(server, factory);
        connector.setHost(host);
        connector.setPort(port);
        // Longer than the chat long poll (pusa-chat.long-poll-timeout), so the connection
        // isn't closed under a client waiting for its next event.
        connector.setIdleTimeout(60000L); // 60-seconds
        server.addConnector(connector);

        // Setup a new Servlet context based on our parameters.
        // See https://github.com/eclipse/jetty.project/issues/3963
        final WebAppContext context = new WebAppContext(server, contextPath, null, null, null, null,
                NO_SESSIONS | NO_SECURITY);
        // No sessions, and no security handlers.
        context.setSessionHandler(null);
        context.setContextPath(contextPath);
        // We do not use JSPs (Java Server Pages) so disable the defaults descriptor
        // which disables loading the JSP engine and slightly improves startup time.
        context.setDefaultsDescriptor(null);

        // The FreeMarker templates and static assets live under the base resource.
        final Resource baseResource = getBaseResourceForRuntime();
        context.setBaseResource(baseResource);
        // Attach the base resource to the context so components that need it at runtime
        // (the FreeMarker template loader) can easily access it.
        context.setAttribute(CONTEXT_ATTRIBUTE_BASE_RESOURCE, baseResource);

        final ServletHolder defaultHolder = new ServletHolder("default", DefaultServlet.class);
        defaultHolder.setAsyncSupported(true); // Async supported = true
        context.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
        context.setInitParameter(DefaultServlet.CONTEXT_INIT + "cacheControl", "public,max-age=3600");
        context.addServlet(defaultHolder, STATIC_SERVLET_MAPPING_UNDER_CONTEXT);
        context.addServlet(defaultHolder, FAVICON_SERVLET_MAPPING_UNDER_CONTEXT);
        context.addServlet(defaultHolder, ROBOTS_SERVLET_MAPPING_UNDER_CONTEXT);

        final ServletHolder curacaoHolder = new ServletHolder("curacao", CuracaoJakartaDispatcherServlet.class);
        curacaoHolder.setAsyncSupported(true); // Async supported = true
        context.addEventListener(new CuracaoJakartaContextListener()); // Required
        context.addServlet(curacaoHolder, CURACAO_SERVLET_MAPPING_UNDER_CONTEXT);

        // Attach the context handler to the server, and go!
        server.setHandler(context);
        server.setStopAtShutdown(true);

        return server;
    }

    private static Resource getBaseResourceForRuntime() throws Exception {
        // In dev, the base resource will be something like "src/main/webapp".
        final Resource srcMainWebApp = Resource.newResource("src/main/webapp");
        if (srcMainWebApp.exists()) {
            return srcMainWebApp;
        }

        // In prod/deployment, the app runs within a JAR and so the base resource
        // will be a "webapp" directory within the fat JAR.
        final URL webApp = Resources.getResource("webapp");
        return Resource.newResource(webApp);
    }

}
