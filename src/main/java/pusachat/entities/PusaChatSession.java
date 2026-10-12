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

package pusachat.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import pusachat.entities.events.PusaChatEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * One client's membership in one room, and the queue of events waiting to be delivered to it.
 */
public final class PusaChatSession implements PusaChatEntity {

    /**
     * The room this session belongs to.
     */
    @JsonProperty("roomId")
    private final UUID roomId_;

    /**
     * The client this session belongs to.
     */
    @JsonProperty("clientId")
    private final UUID clientId_;

    /**
     * A signed token identifying this client in this room. It can't be guessed or forged, so
     * one client can't wait for or post events on behalf of another; a client only learns its
     * own token, when it registers.
     */
    @JsonProperty("token")
    private final String token_;

    @JsonProperty("lastAccessed")
    private volatile Instant lastAccessed_;

    private final BlockingQueue<PusaChatEvent> eventQueue_;

    public PusaChatSession(
            final UUID roomId,
            final UUID clientId,
            final String token) {
        roomId_ = roomId;
        clientId_ = clientId;
        token_ = token;
        lastAccessed_ = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        eventQueue_ = new LinkedBlockingQueue<>();
    }

    public UUID getRoomId() {
        return roomId_;
    }

    public UUID getClientId() {
        return clientId_;
    }

    public String getToken() {
        return token_;
    }

    public BlockingQueue<PusaChatEvent> getEventQueue() {
        return eventQueue_;
    }

    public Instant getLastAccessed() {
        return lastAccessed_;
    }

    /**
     * Marks this session as accessed now, keeping the client in the room.
     */
    public PusaChatSession wasAccessed() {
        lastAccessed_ = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return this;
    }

}
