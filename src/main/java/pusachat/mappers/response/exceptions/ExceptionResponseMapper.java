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

package pusachat.mappers.response.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.exceptions.CuracaoException;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.mappers.AbstractFreeMarkerContentResponseMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pusachat.components.config.PusaChatConfig;
import pusachat.components.freemarker.FreeMarkerContentRenderer;
import pusachat.components.jackson.PusaChatJacksonObjectMapper;
import pusachat.entities.ApiErrorResponse;
import pusachat.mappers.response.PusaChatJsonEntity;

import javax.annotation.Nonnull;

import static curacao.core.servlet.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static pusachat.components.freemarker.PusaChatFreeMarkerContent.newHtmlBuilder;

/**
 * Renders anything a controller throws. API requests (every path ending in {@code .json}) get
 * a JSON error body; pages get an HTML error page. A {@link CuracaoException.WithStatus},
 * which includes every {@link pusachat.exceptions.PusaChatException} and Curacao's own routing
 * errors, carries its own HTTP status; anything else unexpected is logged (Curacao otherwise
 * swallows it at DEBUG) and returned as a 500.
 *
 * <p>This is deliberately one mapper for {@link Exception} rather than one per exception type:
 * Curacao does not prefer the most specific mapper, so a separate catch-all would shadow the
 * others.
 */
@Mapper
public final class ExceptionResponseMapper extends AbstractFreeMarkerContentResponseMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionResponseMapper.class);

    private static final String API_PATH_SUFFIX = ".json";

    private final PusaChatConfig pusaChatConfig_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public ExceptionResponseMapper(
            final PusaChatConfig pusaChatConfig,
            final PusaChatJacksonObjectMapper pusaChatJacksonObjectMapper,
            final FreeMarkerContentRenderer fmcRenderer) {
        super(fmcRenderer);
        pusaChatConfig_ = pusaChatConfig;
        objectMapper_ = pusaChatJacksonObjectMapper.getObjectMapper();
    }

    @Override
    public void render(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final Exception exception) throws Exception {
        final int statusCode;
        if (exception instanceof CuracaoException.WithStatus withStatus) {
            statusCode = withStatus.getEntity().getStatus();
        } else {
            LOG.error("Unhandled exception while processing request.", exception);
            statusCode = SC_INTERNAL_SERVER_ERROR;
        }

        final String message = StringUtils.defaultIfBlank(exception.getMessage(), null);
        final String stackTrace = (pusaChatConfig_.isDevMode())
                ? ExceptionUtils.getStackTrace(exception) : null;

        final String requestUri = StringUtils.defaultString(context.getRequest().getRequestURI());
        if (requestUri.endsWith(API_PATH_SUFFIX)) {
            renderEntity(response, new PusaChatJsonEntity(objectMapper_,
                    new ApiErrorResponse(statusCode, message, stackTrace)));
        } else {
            final FreeMarkerContent page = newHtmlBuilder("templates/error.ftl", statusCode)
                    .withAttr("statusCode", statusCode)
                    .withAttr("statusMessage", HttpStatus.getMessage(statusCode))
                    .withAttr("message", message)
                    .build();
            renderFreeMarkerContent(response, page);
        }
    }

}
