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

package pusachat.entities.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import pusachat.entities.PusaChatEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Something that happened in a room, delivered to every client in it. Serialized with its
 * {@code type} so the front-end knows what to do with it.
 */
public interface PusaChatEvent extends PusaChatEntity {

    enum EventType {

        /**
         * A message was sent.
         */
        MESSAGE,

        /**
         * A message was deleted.
         */
        DELETE,

        /**
         * The number of active and inactive clients in the room changed.
         */
        CLIENT_STATUS,

        /**
         * A client joined the room.
         */
        CLIENT_JOINED,

        /**
         * A client left the room: it stopped polling for events, and was removed.
         */
        CLIENT_LEFT,

        /**
         * Someone started or stopped typing.
         */
        TYPING,

        /**
         * Nothing happened; returned when a client's long poll times out.
         */
        NOOP

    }

    @JsonProperty("id")
    UUID getId();

    @JsonProperty("when")
    Instant getWhen();

    @JsonProperty("type")
    EventType getType();

    abstract class AbstractPusaChatEventBuilder {

        /**
         * Every event gets a new, unique ID.
         */
        protected final UUID id_;

        /**
         * When the event happened, truncated to milliseconds (see
         * {@link pusachat.components.jackson.PusaChatJacksonObjectMapper}).
         */
        protected final Instant when_;

        public AbstractPusaChatEventBuilder() {
            id_ = UUID.randomUUID();
            when_ = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        }

    }

}
