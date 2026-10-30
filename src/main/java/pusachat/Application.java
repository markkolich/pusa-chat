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

import com.kolich.buildversion.BuildVersion;
import com.typesafe.config.ConfigFactory;
import curacao.jetty11.CuracaoJettyServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import pusachat.components.config.PusaChatConfig;

import java.time.Duration;

/**
 * Entry point. Boots a Jetty web-server hosting the Curacao dispatcher (the pages and the
 * chat API) and static assets; Curacao in turn instantiates every component under the
 * {@code pusachat} boot-package.
 */
public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String THREAD_NAME_FORMAT = "jetty-pusachat-%d";

    private static final String STATIC_PATH_SPEC = "/static/*";
    private static final String FAVICON_PATH_SPEC = "/favicon.ico";
    private static final String ROBOTS_PATH_SPEC = "/robots.txt";

    private static final String STATIC_SERVLET_DIR_ALLOWED = "dirAllowed";
    private static final String STATIC_SERVLET_CACHE_CONTROL = "cacheControl";
    private static final String STATIC_CACHE_CONTROL = "public,max-age=3600";

    /**
     * Longer than the chat long poll (pusa-chat.long-poll-timeout), so the connection isn't
     * closed under a client waiting for its next event.
     */
    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(60L);

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

        // One static file servlet, from the base resource, for the assets and the two root files.
        final ServletHolder staticFiles = createStaticServletHolder();

        return new CuracaoJettyServer.Builder(contextPath)
                .setThreadNameFormat(THREAD_NAME_FORMAT)
                .setHost(host)
                .setPort(port)
                .setIdleTimeout(IDLE_TIMEOUT)
                .addServlet(staticFiles, STATIC_PATH_SPEC)
                .addServlet(staticFiles, FAVICON_PATH_SPEC)
                .addServlet(staticFiles, ROBOTS_PATH_SPEC)
                .build();
    }

    private static ServletHolder createStaticServletHolder() {
        final ServletHolder staticFiles = new ServletHolder("default", DefaultServlet.class);
        staticFiles.setAsyncSupported(true);

        // Jetty's DefaultServlet lists directories unless told not to.
        staticFiles.setInitParameter(STATIC_SERVLET_DIR_ALLOWED, Boolean.FALSE.toString());
        staticFiles.setInitParameter(STATIC_SERVLET_CACHE_CONTROL, STATIC_CACHE_CONTROL);

        return staticFiles;
    }

}
