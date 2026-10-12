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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pusachat.entities.events.ClientStatus;
import pusachat.entities.events.Delete;
import pusachat.entities.events.Message;
import pusachat.entities.events.PusaChatEvent;
import pusachat.entities.events.Typing;
import pusachat.exceptions.ClientNotRegisteredWithChatRoomException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/**
 * A chat room: the clients in it, who's typing or inactive, and its recent messages. Only the
 * token, name and creation time are serialized; everything else stays on the server.
 */
public final class ChatRoom implements PusaChatEntity {

    private static final Logger LOG = LoggerFactory.getLogger(ChatRoom.class);

    private static final Pattern VALID_ROOM_NAME_PATTERN = Pattern.compile("\\A[a-zA-Z0-9_\\-]{1,36}\\Z");

    private final UUID roomId_;

    /**
     * The signed room ID; what clients use to refer to the room.
     */
    @JsonProperty("token")
    private final String token_;

    @JsonProperty("createdOn")
    private final Instant createdOn_;

    @JsonProperty("name")
    private final String name_;

    private final Map<UUID, PusaChatSession> sessions_;

    private final Set<UUID> typing_;

    private final Set<UUID> inactive_;

    private final ChatLog log_;

    /**
     * Runs this room's events one at a time, in the order they were posted, so every client sees
     * them in the same order they happened (and a delete never overtakes its message). It's a
     * sequential view of a shared pool: an idle room holds no thread.
     */
    private final Executor eventExecutor_;

    public ChatRoom(
            final String name,
            final UUID roomId,
            final String token,
            final int maxMessages,
            final Executor eventExecutor) {
        roomId_ = roomId;
        token_ = token;
        createdOn_ = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        name_ = name;
        sessions_ = new ConcurrentHashMap<>();
        typing_ = new HashSet<>();
        inactive_ = new HashSet<>();
        log_ = new ChatLog(maxMessages);
        eventExecutor_ = eventExecutor;
    }

    public UUID getId() {
        return roomId_;
    }

    public String getToken() {
        return token_;
    }

    public Instant getCreatedOn() {
        return createdOn_;
    }

    public String getName() {
        return name_;
    }

    public synchronized PusaChatSession registerClient(
            final UUID clientId,
            final String token) {
        return sessions_.computeIfAbsent(clientId, id -> new PusaChatSession(roomId_, id, token));
    }

    public PusaChatSession getSessionByClientId(
            final UUID clientId) {
        final PusaChatSession session = sessions_.get(clientId);
        if (session == null) {
            // The client never registered with this room, or was removed for inactivity.
            throw new ClientNotRegisteredWithChatRoomException("Client not registered with room (clientId="
                    + clientId + ", roomId=" + roomId_ + ")");
        }
        return session;
    }

    /**
     * Delivers an event to every client in the room, after any events posted before it. Returns
     * right away; the request that caused the event doesn't wait on delivery, however busy the
     * room is.
     */
    public void postEvent(
            final PusaChatEvent event) {
        eventExecutor_.execute(() -> {
            for (final PusaChatSession session : sessions_.values()) {
                session.getEventQueue().add(event);
            }
            // Only messages (and their deletion) are kept in the room's log; any other event is
            // only for whoever is in the room right now.
            if (event instanceof Message message) {
                log_.addMessage(message);
            } else if (event instanceof Delete delete) {
                log_.deleteMessage(delete.getMessageId());
            }
        });
    }

    public synchronized ClientStatus getClientStatus() {
        return new ClientStatus.Builder()
                .setActiveClients(sessions_.size())
                .setInactiveClients(inactive_.size())
                .build();
    }

    public synchronized Typing getTyping() {
        return new Typing.Builder()
                .setWhosTyping(typing_)
                .build();
    }

    public synchronized Typing updateTyping(
            final UUID clientId,
            final boolean isTyping) {
        if (isTyping) {
            typing_.add(clientId);
        } else {
            typing_.remove(clientId);
        }
        return getTyping();
    }

    public synchronized ClientStatus updateInactivity(
            final UUID clientId,
            final boolean isActive) {
        if (isActive) {
            inactive_.remove(clientId);
        } else {
            inactive_.add(clientId);
        }
        return getClientStatus();
    }

    /**
     * Removes every client that hasn't been heard from since {@code expiry}.
     *
     * @return the IDs of the clients removed, if any
     */
    public synchronized List<UUID> removeInactiveClients(
            final Instant expiry) {
        final List<UUID> removed = new ArrayList<>();
        final Iterator<PusaChatSession> it = sessions_.values().iterator();
        while (it.hasNext()) {
            final PusaChatSession session = it.next();
            if (session.getLastAccessed().isBefore(expiry)) {
                LOG.info("Client left room (clientId={}, roomId={}, lastAccessed={})",
                        session.getClientId(), roomId_, session.getLastAccessed());
                it.remove();
                typing_.remove(session.getClientId());
                inactive_.remove(session.getClientId());
                removed.add(session.getClientId());
            }
        }
        return removed;
    }

    public ChatLog getChatLog() {
        return log_;
    }

    public static boolean isValidRoomName(
            final String roomName) {
        return roomName != null && VALID_ROOM_NAME_PATTERN.matcher(roomName).matches();
    }

}
