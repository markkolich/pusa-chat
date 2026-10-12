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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import pusachat.entities.PusaChatEntity;
import pusachat.entities.events.Message;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PusaChatJacksonObjectMapperTest {

    private final ObjectMapper mapper_ = new PusaChatJacksonObjectMapper().getObjectMapper();

    @Test
    public void instantsAreIsoStringsTheFrontEndCanParse() throws Exception {
        // Three fractional digits, or none when the milliseconds are zero: the only shapes
        // jquery.localtime accepts. And only "when": not the status, nor the other getter.
        assertEquals("{\"when\":\"2026-10-12T02:42:20.443Z\"}",
                mapper_.writeValueAsString(new When(Instant.parse("2026-10-12T02:42:20.443Z"))));
        assertEquals("{\"when\":\"2026-10-12T02:42:20.400Z\"}",
                mapper_.writeValueAsString(new When(Instant.parse("2026-10-12T02:42:20.400Z"))));
        assertEquals("{\"when\":\"2026-10-12T02:42:20Z\"}",
                mapper_.writeValueAsString(new When(Instant.parse("2026-10-12T02:42:20Z"))));
    }

    @Test
    public void eventsSerializeOnlyTheirApiFields() throws Exception {
        final Message message = new Message.Builder()
                .setFrom("alice")
                .setHtml("hi")
                .build();

        final JsonNode json = mapper_.readTree(mapper_.writeValueAsString(message));

        assertEquals(Set.of("id", "when", "type", "from", "html"), Sets.newHashSet(json.fieldNames()));
        assertEquals(message.getId().toString(), json.get("id").asText());
        assertEquals("MESSAGE", json.get("type").asText());
        assertEquals("alice", json.get("from").asText());
        assertEquals("hi", json.get("html").asText());
        // At most milliseconds, whenever the event happened.
        final String when = json.get("when").asText();
        assertTrue(when.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z"), when);
    }

    private static final class When implements PusaChatEntity {

        @JsonProperty("when")
        private final Instant when_;

        // Has a getter, but isn't annotated: not part of the API.
        private final String notPartOfTheApi_ = "secret";

        private When(
                final Instant when) {
            when_ = when;
        }

        public String getNotPartOfTheApi() {
            return notPartOfTheApi_;
        }

    }

}
