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

package pusachat.components.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import curacao.annotations.Component;

/**
 * The one Jackson {@link ObjectMapper} every API entity is rendered with (see
 * {@link pusachat.mappers.response.PusaChatEntityResponseMapper}).
 *
 * <p>Instants are written as ISO-8601 strings, e.g. {@code 2026-10-12T02:42:20.443Z}. The
 * front-end's date parser (jquery.localtime) only accepts exactly three fractional digits, or
 * none, so every {@link java.time.Instant} in an entity must be truncated to milliseconds.
 */
@Component
public final class PusaChatJacksonObjectMapper {

    private final ObjectMapper objectMapper_;

    public PusaChatJacksonObjectMapper() {
        objectMapper_ = JsonMapper.builder()
                // Only fields (or methods) annotated with @JsonProperty are serialized; entities have
                // plenty of getters, and in-memory state, that aren't part of the API.
                .visibility(PropertyAccessor.ALL, Visibility.NONE)
                // Write out Instants as human readable ISO-8601 strings.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Required to support Instant typed entity fields.
                .addModule(new JavaTimeModule())
                .build();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper_;
    }

}
