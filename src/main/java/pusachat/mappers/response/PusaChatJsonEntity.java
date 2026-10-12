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
import curacao.entities.CuracaoEntity;
import pusachat.entities.PusaChatEntity;

import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;

/**
 * Adapts a {@link PusaChatEntity} to a {@link CuracaoEntity}, so it can be rendered
 * like any other Curacao entity: the entity's status, a JSON content type, and the entity
 * written with the given mapper (as UTF-8).
 */
public final class PusaChatJsonEntity implements CuracaoEntity {

    private static final String JSON_UTF_8_TYPE = JSON_UTF_8.toString();

    private final ObjectMapper objectMapper_;
    private final PusaChatEntity entity_;

    public PusaChatJsonEntity(
            final ObjectMapper objectMapper,
            final PusaChatEntity entity) {
        objectMapper_ = checkNotNull(objectMapper, "Object mapper cannot be null.");
        entity_ = checkNotNull(entity, "Entity cannot be null.");
    }

    @Override
    public int getStatus() {
        return entity_.getStatus();
    }

    @Override
    public String getContentType() {
        return JSON_UTF_8_TYPE;
    }

    @Override
    public void write(
            final OutputStream os) throws Exception {
        objectMapper_.writeValue(os, entity_);
    }

}
