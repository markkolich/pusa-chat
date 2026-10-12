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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pusachat.components.config.PusaChatConfig;
import pusachat.entities.ChatRoom;
import pusachat.entities.events.ClientLeft;
import pusachat.exceptions.BadChatTokenException;
import pusachat.exceptions.InvalidRoomNameException;
import pusachat.exceptions.RoomNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Every chat room, held in memory: by ID, and by name (the "room key" people type in). A
 * background thread removes clients that stop polling for events, and tells the rest of the
 * room who left.
 */
@Component
public final class ChatRooms implements ComponentDestroyable {

    private static final Logger LOG = LoggerFactory.getLogger(ChatRooms.class);

    private final TokenSigner signer_;
    private final int maxCachedMessagesPerRoom_;

    private final Map<UUID, ChatRoom> rooms_;
    private final Map<String, ChatRoom> roomsByName_;

    /**
     * Delivers events to clients for every room. Each room runs its own events in order on this
     * pool (see {@link ChatRoom#postEvent}).
     */
    private final ExecutorService postEventPool_;

    private final ScheduledExecutorService cleaner_;

    @Injectable
    public ChatRooms(
            final PusaChatConfig config,
            final TokenSigner signer) {
        signer_ = signer;
        maxCachedMessagesPerRoom_ = config.getMaxCachedMessagesPerRoom();
        rooms_ = new ConcurrentHashMap<>();
        roomsByName_ = new ConcurrentHashMap<>();

        postEventPool_ = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("post-event-pool-%d")
                .build());

        final Duration removeInactiveUsersAfter = config.getRemoveInactiveUsersAfter();
        cleaner_ = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("inactive-user-cleaner-%d")
                .build());
        cleaner_.scheduleAtFixedRate(() -> removeInactiveClients(removeInactiveUsersAfter),
                removeInactiveUsersAfter.toMillis(), removeInactiveUsersAfter.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        cleaner_.shutdownNow();
        postEventPool_.shutdownNow();
    }

    public ChatRoom getRoom(
            final UUID roomId) {
        final ChatRoom room = rooms_.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException("Found no room for ID: " + roomId);
        }
        return room;
    }

    /**
     * Returns the room a room token was issued for.
     */
    public ChatRoom getRoomFromToken(
            final String roomToken) {
        final String roomId = signer_.verify(roomToken);
        try {
            return getRoom(UUID.fromString(roomId));
        } catch (final IllegalArgumentException e) {
            throw new BadChatTokenException("Token is not a room token: " + roomToken);
        }
    }

    /**
     * Returns the room with the given name (room key), creating it first if asked to.
     */
    public ChatRoom getRoomByName(
            final String roomName,
            final boolean create) {
        if (!ChatRoom.isValidRoomName(roomName)) {
            throw new InvalidRoomNameException("Invalid room name: " + roomName);
        }
        if (!create) {
            final ChatRoom room = roomsByName_.get(roomName);
            if (room == null) {
                throw new RoomNotFoundException("Room not found: " + roomName);
            }
            return room;
        }
        return roomsByName_.computeIfAbsent(roomName, name -> {
            final UUID roomId = UUID.randomUUID();
            final ChatRoom room = new ChatRoom(name, roomId, signer_.sign(roomId.toString()),
                    maxCachedMessagesPerRoom_, MoreExecutors.newSequentialExecutor(postEventPool_));
            rooms_.put(roomId, room);
            LOG.info("Room created (roomId={}, name={})", roomId, name);
            return room;
        });
    }

    private void removeInactiveClients(
            final Duration removeInactiveUsersAfter) {
        try {
            final Instant expiry = Instant.now().minus(removeInactiveUsersAfter);
            for (final ChatRoom room : rooms_.values()) {
                final List<UUID> removed = room.removeInactiveClients(expiry);
                if (!removed.isEmpty()) {
                    // Let everyone left in the room know.
                    for (final UUID clientId : removed) {
                        room.postEvent(new ClientLeft.Builder()
                                .setClientId(clientId)
                                .build());
                    }
                    room.postEvent(room.getClientStatus());
                    room.postEvent(room.getTyping());
                }
            }
        } catch (final Exception e) {
            // Never let an exception escape: it would cancel every future run.
            LOG.warn("Failed to remove inactive users.", e);
        }
    }

}
