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

package pusachat.mappers.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;
import pusachat.components.jackson.PusaChatJacksonObjectMapper;
import pusachat.entities.PusaChatEntity;

import javax.annotation.Nonnull;

/**
 * Renders every API entity a controller returns as JSON.
 */
@Mapper
public final class PusaChatEntityResponseMapper
        extends AbstractControllerReturnTypeMapper<PusaChatEntity> {

    private final ObjectMapper objectMapper_;

    @Injectable
    public PusaChatEntityResponseMapper(
            final PusaChatJacksonObjectMapper pusaChatJacksonObjectMapper) {
        objectMapper_ = pusaChatJacksonObjectMapper.getObjectMapper();
    }

    @Override
    public void render(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final PusaChatEntity entity) throws Exception {
        renderEntity(response, new PusaChatJsonEntity(objectMapper_, entity));
    }

}
