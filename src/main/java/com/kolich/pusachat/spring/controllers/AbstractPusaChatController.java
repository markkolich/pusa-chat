/**
 * Copyright (c) 2013 Mark S. Kolich
 * http://mark.koli.ch
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

package com.kolich.pusachat.spring.controllers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.pusachat.entities.ChatRoom.isValidRoomName;
import static com.kolich.pusachat.util.MessageToHtml.makeHyperlinks;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.entities.ChatLog;
import com.kolich.pusachat.entities.ChatRoom;
import com.kolich.pusachat.entities.PusaChatSession;
import com.kolich.pusachat.entities.events.ClientJoined;
import com.kolich.pusachat.entities.events.ClientStatus;
import com.kolich.pusachat.entities.events.Delete;
import com.kolich.pusachat.entities.events.Message;
import com.kolich.pusachat.entities.events.PusaChatEvent;
import com.kolich.pusachat.entities.events.Typing;
import com.kolich.pusachat.exceptions.BadChatTokenException;
import com.kolich.pusachat.exceptions.InvalidRoomNameException;
import com.kolich.pusachat.spring.beans.ChatRooms;

public abstract class AbstractPusaChatController {
	
	/**
	 * The default global delimiter that's used to logically separate
	 * a room {@link UUID} and a client {@link UUID} in a signed token.
	 */
	private static final String TOKEN_DELIMITER = "_";
			
	protected final Logger logger_;
	
	protected final KolichStringSigner signer_;
	protected final ChatRooms rooms_;
	
	/**
	 * A thread pool is used to "post" events to the queues of clients.
	 * This is so that the caller does not have to wait on the infrastructre
	 * posting events, which may or may not be a slow/costly operation
	 * depending on how busy the room is.
	 */
	private static final ExecutorService postEventPool__ =
		newCachedThreadPool(
			new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("post-event-pool-%s")
				.setPriority(Thread.MAX_PRIORITY)
				.build());
	
	protected AbstractPusaChatController(Logger logger,
		KolichStringSigner signer,
		ChatRooms rooms) {
		logger_ = logger;
		signer_ = signer;
		rooms_ = rooms;
	}
		
	protected ChatRoom getRoom(final UUID roomId) {
		return rooms_.getRoom(roomId);
	}
	
	protected ChatRoom getRoomFromKey(final String key, final boolean create) {
		// Validate the room name; must be properly formed.
		if(!isValidRoomName(key)) {
			throw new InvalidRoomNameException("Invalid room name: " + key);
		}
		return rooms_.getRoomFromKey(key, create);
	}
	
	protected PusaChatSession getSession(final String token) {		
		return getRoom(getRoomIdFromToken(token))
			.getSessionByClientId(getClientIdFromToken(token))
			// Update the last accessed time too.
			.wasAccessed();
	}
	
	protected void postEvent(final ChatRoom room, final PusaChatEvent event) {
		// Post the event to the queue in a separate thread.
		postEventPool__.execute(new Runnable() {
			@Override
			public void run() {
				// Post the event to all clients in the room.
				room.postEvent(event);
				// Only need to persist events of type "Message" to log
				// chat messages -- any other event is dropped on the floor.
				if(event instanceof Message) {
					room.getChatLog().addMessage((Message)event);
				} else if(event instanceof Delete) {
					room.getChatLog().deleteMessage((Delete)event);
				}
			}
		});
	}
	
	protected void postEvent(final UUID roomId, final PusaChatEvent event) {
		postEvent(getRoom(roomId), event);
	}
	
	protected PusaChatSession registerClient(final UUID roomId) {
		final ChatRoom room = getRoom(roomId);
		final UUID clientId = randomUUID();
		final PusaChatSession session = room.registerClient(
			// Register the randomly generated new client UUID.
			clientId,
			// Also pass in the token which is a digitally signed
			// String containing the room UUID and client UUID. This
			// is an unguessable ("secure") signed unique identifier.
			createToken(room.getId(), clientId));
		// Post an event that a new client with the given ID joined
		// the chat room.
		postEvent(room, new ClientJoined(clientId));
		// Also post an event containing the new status of the room
		// listing both active and inactive clients.
		postEvent(room, room.getClientStatus());
		return session;
	}
		
	protected Message postMessage(final UUID roomId, final UUID clientId,
		final String message) {
		// Build a new message.
		final Message m = new Message(clientId.toString(),
			makeHyperlinks(escapeHtml4(message)));
		// Post the event to the room so it's picked up by any waiting clients.
		postEvent(roomId, m);
		return m;
	}
	
	protected Delete postDeleteMessage(final UUID roomId, final UUID clientId,
		final String messageId) {
		checkNotNull(messageId, "Oops, message ID to delete cannot be null!");
		final Delete delete = new Delete(fromString(messageId));
		// Post the event to the room so it's picked up by any waiting clients.
		postEvent(roomId, delete);
		return delete;
	}
	
	protected Typing setTypingStatus(final UUID roomId, final UUID clientId,
		final boolean isTyping) {
		final ChatRoom room = getRoom(roomId);
		final Typing typing = room.updateTyping(clientId, isTyping);
		postEvent(room, typing);
		return typing;
	}
	
	protected ClientStatus setInactivityStatus(final UUID roomId,
		final UUID clientId, final boolean isActive) {
		final ChatRoom room = getRoom(roomId);
		final ClientStatus status = room.updateInactivity(clientId, isActive);
		postEvent(room, status);
		return status;
	}
	
	protected ChatLog getChatLog(final UUID roomId) {
		return getRoom(roomId).getChatLog();
	}
		
	protected String createToken(final UUID roomId, final UUID clientId) {
		checkNotNull(roomId, "Oops, room UUID cannot be null!");
		checkNotNull(clientId, "Oops, client UUID cannot be null!");
		// Probably don't need to call toString() on the UUID's but
		// I'm being pedantic.
		return signer_.sign(roomId.toString() + TOKEN_DELIMITER +
			clientId.toString());
	}
	
	protected UUID getRoomIdFromToken(final String token) {
		try {
			return fromString(getTokensFromPayload(signer_.isValid(token))[0]);
		} catch (Exception e) {
			throw new BadChatTokenException("Failed to extract room " +
				"UUID from token: " + token, e);
		}
	}
	
	protected UUID getClientIdFromToken(final String token) {
		try {
			return fromString(getTokensFromPayload(signer_.isValid(token))[1]);
		} catch (Exception e) {
			throw new BadChatTokenException("Failed to extract client " +
				"UUID from token: " + token, e);
		}
	}
		
	private static final String[] getTokensFromPayload(final String payload) {
		checkNotNull(payload, "Oops, token payload cannot be null!");
		return payload.split(TOKEN_DELIMITER, 2 /* important */);
	}
	
}
