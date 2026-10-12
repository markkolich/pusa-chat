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

package pusachat.components.chat;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pusachat.components.config.PusaChatConfig;
import pusachat.entities.ChatLog;
import pusachat.entities.ChatRoom;
import pusachat.entities.PusaChatSession;
import pusachat.entities.events.ClientJoined;
import pusachat.entities.events.ClientStatus;
import pusachat.entities.events.Delete;
import pusachat.entities.events.Message;
import pusachat.entities.events.NoOp;
import pusachat.entities.events.PusaChatEvent;
import pusachat.entities.events.Typing;
import pusachat.exceptions.BadChatTokenException;
import pusachat.util.MessageToHtml;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * What a client does in a room: register, send and delete messages, report typing and
 * inactivity, and wait for the next event. Clients identify themselves with the signed client
 * token they get when they register, which carries both the room ID and the client ID.
 */
@Component
public final class ChatSessions {

    private static final Logger LOG = LoggerFactory.getLogger(ChatSessions.class);

    /**
     * Separates the room ID and client ID in a client token's payload.
     */
    private static final String TOKEN_DELIMITER = "_";

    private final ChatRooms rooms_;
    private final TokenSigner signer_;
    private final long longPollTimeoutMs_;

    @Injectable
    public ChatSessions(
            final PusaChatConfig config,
            final ChatRooms rooms,
            final TokenSigner signer) {
        rooms_ = rooms;
        signer_ = signer;
        longPollTimeoutMs_ = config.getLongPollTimeout().toMillis();
    }

    /**
     * Adds a new client to the room a room token was issued for, and tells everyone in the
     * room (the new client included).
     */
    public PusaChatSession register(
            final String roomToken) {
        final ChatRoom room = rooms_.getRoomFromToken(roomToken);
        final UUID clientId = UUID.randomUUID();
        final PusaChatSession session = room.registerClient(clientId, createClientToken(room.getId(), clientId));
        LOG.debug("Client joined room (clientId={}, roomId={})", clientId, room.getId());
        room.postEvent(new ClientJoined.Builder()
                .setClientId(clientId)
                .build());
        room.postEvent(room.getClientStatus());
        return session;
    }

    /**
     * Returns the session a client token was issued for, marking it as accessed so the client
     * isn't removed for inactivity.
     */
    public PusaChatSession getSession(
            final String clientToken) {
        final List<UUID> ids = parseClientToken(clientToken);
        return rooms_.getRoom(ids.get(0))
                .getSessionByClientId(ids.get(1))
                .wasAccessed();
    }

    /**
     * Waits up to the long poll timeout for the client's next event, returning a NOOP if
     * nothing happened; the client then asks again.
     */
    public PusaChatEvent nextEvent(
            final PusaChatSession session) throws InterruptedException {
        final PusaChatEvent event = session.getEventQueue().poll(longPollTimeoutMs_, TimeUnit.MILLISECONDS);
        return (event != null) ? event : new NoOp.Builder().build();
    }

    public ChatLog getChatLog(
            final PusaChatSession session) {
        return rooms_.getRoom(session.getRoomId()).getChatLog();
    }

    public Message postMessage(
            final PusaChatSession session,
            final String text) {
        final Message message = new Message.Builder()
                .setFrom(session.getClientId().toString())
                .setHtml(MessageToHtml.makeHyperlinks(StringEscapeUtils.escapeHtml4(text)))
                .build();
        rooms_.getRoom(session.getRoomId()).postEvent(message);
        return message;
    }

    public Delete deleteMessage(
            final PusaChatSession session,
            final UUID messageId) {
        final Delete delete = new Delete.Builder()
                .setMessageId(messageId)
                .build();
        rooms_.getRoom(session.getRoomId()).postEvent(delete);
        return delete;
    }

    public Typing setTyping(
            final PusaChatSession session,
            final boolean isTyping) {
        final ChatRoom room = rooms_.getRoom(session.getRoomId());
        final Typing typing = room.updateTyping(session.getClientId(), isTyping);
        room.postEvent(typing);
        return typing;
    }

    public ClientStatus setActive(
            final PusaChatSession session,
            final boolean isActive) {
        final ChatRoom room = rooms_.getRoom(session.getRoomId());
        final ClientStatus status = room.updateInactivity(session.getClientId(), isActive);
        room.postEvent(status);
        return status;
    }

    private String createClientToken(
            final UUID roomId,
            final UUID clientId) {
        checkNotNull(roomId, "Room ID cannot be null.");
        checkNotNull(clientId, "Client ID cannot be null.");
        return signer_.sign(roomId + TOKEN_DELIMITER + clientId);
    }

    /**
     * Returns the room ID and client ID in a client token.
     */
    private List<UUID> parseClientToken(
            final String clientToken) {
        final String[] ids = signer_.verify(clientToken).split(TOKEN_DELIMITER, 2);
        try {
            return List.of(UUID.fromString(ids[0]), UUID.fromString(ids[1]));
        } catch (final IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new BadChatTokenException("Token is not a client token: " + clientToken);
        }
    }

}
