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

package com.kolich.pusachat.spring.beans;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.entities.ChatRoom;
import com.kolich.pusachat.entities.events.PusaChatEvent;
import com.kolich.pusachat.exceptions.RoomNotFoundException;
import com.kolich.pusachat.spring.PusaChatProperties;

public final class ChatRooms implements InitializingBean, DisposableBean {
	
	private static final Logger logger__ =
		LoggerFactory.getLogger(ChatRooms.class);
	
	private Map<UUID, ChatRoom> chatRooms_;
	
	private Map<String, UUID> keysToRooms_;
	private Map<UUID, String> roomsToKeys_;
	
	private PusaChatProperties properties_;
	private KolichStringSigner signer_;
	
	private ScheduledExecutorService executor_;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		chatRooms_ = new ConcurrentHashMap<UUID, ChatRoom>();
		// Reverse maps.
		keysToRooms_ = new ConcurrentHashMap<String, UUID>();
		roomsToKeys_ = new ConcurrentHashMap<UUID, String>();
		// Extract a configuration property that instructions how often
		// the chat room cleaner thread should wake up and remove "dead"
		// users from all of the rooms.
		final long removeInactiveUsersAfterMs =
			properties_.getRemoveInactiveAfter();
		// Setup a new thread factory builder.
		executor_ = newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("inactive-user-cleaner-%d")
				.build());
		// Schedule a new cleaner at a "fixed" interval.
		executor_.scheduleAtFixedRate(
			new InactiveUserCleanerExecutor(this, removeInactiveUsersAfterMs),
			0L,  // initial delay
			removeInactiveUsersAfterMs, // repeat every
			MILLISECONDS); // units
	}
	
	@Override
	public void destroy() throws Exception {
		if(executor_ != null) {
			executor_.shutdown();
		}
	}
	
	public synchronized ChatRoom getRoom(final UUID roomId) {
		final ChatRoom cr = chatRooms_.get(roomId);
		if(cr == null) {
			throw new RoomNotFoundException("Found no room for chat UUID: " +
				roomId);
		}
		return cr;
	}
	
	public synchronized ChatRoom getRoom(final String roomId) {
		try {
			return getRoom(UUID.fromString(roomId));
		} catch (IllegalArgumentException e) {
			throw new RoomNotFoundException("Malformed chat room UUID: " +
				roomId, e);
		}
	}
	
	public synchronized ChatRoom newRoom(final String roomName) {
		final UUID roomId = UUID.randomUUID();
		final ChatRoom cr = new ChatRoom(roomName,
			// The room's UUID.
			roomId,
			// Digitally signed room UUID (a.k.a., room token).
			signer_.sign(roomId.toString()),
			// The maximum number of cached messages to keep in memory.
			properties_.getMaxCachedMessagePerRoom());
		chatRooms_.put(cr.getId(), cr);
		keysToRooms_.put(roomName, cr.getId());
		roomsToKeys_.put(cr.getId(), roomName);
		return cr;
	}
	
	public synchronized ChatRoom getRoomFromKey(final String roomName,
		final boolean create) {
		ChatRoom cr = null;
		UUID id = null;
		if((id = keysToRooms_.get(roomName)) != null) {
			try {
				cr = getRoom(id);
			} catch (RoomNotFoundException nfe) { }
		}
		// If we were not asked to create one, and nothing was found, bail.
		if(cr == null && !create) {
			throw new RoomNotFoundException("Room not found: " + roomName);
		} else if(cr == null && create) {
			cr = newRoom(roomName);
		}
		return cr;
	}
	
	public synchronized Set<UUID> getAllRooms() {
		return Collections.unmodifiableSet(chatRooms_.keySet());
	}
		
	public synchronized void deleteRoom(final UUID roomId) {
		chatRooms_.remove(roomId);
		final String roomName;
		if((roomName = roomsToKeys_.remove(roomId)) != null) {
			keysToRooms_.remove(roomName);
		}
	}
	
	public void setProperties(PusaChatProperties properties) {
		properties_ = properties;
	}
	
	public void setSigner(KolichStringSigner signer) {
		signer_ = signer;
	}
	
	private final class InactiveUserCleanerExecutor implements Runnable {
		
		/**
		 * How "old" inactive users can be before they are considered
		 * absent from the room.
		 */
		private final long expiry_;
		
		private final ChatRooms rooms_;
		
		private InactiveUserCleanerExecutor(final ChatRooms rooms,
			final long expiry) {
			rooms_ = rooms;
			expiry_ = expiry;
		}

		@Override
		public void run() {
			try {
				// Destroy all users who haven't ping'ed the room within X-seconds.
				final Date when = new Date(currentTimeMillis() - expiry_);
				// List all rooms, and cleanup each one.
				final Set<UUID> rooms = rooms_.getAllRooms();
				for(final UUID roomId : rooms) {
					try {
						final ChatRoom room = rooms_.getRoom(roomId);
						// Remove all clients from the room who haven't pinged
						// us since "when", the expiry.
						if(room.removeInactiveClients(when)) {
							// If we did remove a client, then let everyone in
							// the room know about the event.
							room.postEvents(Arrays.asList(new PusaChatEvent[]{
								room.getClientStatus(),
								room.getTyping()
							}));
						}
					} catch (RoomNotFoundException rnfe) {
						logger__.warn("Room (" + roomId.toString() + ") was " +
							"in rooms list, but seems to have disappeared; " +
								"can't remove inactive users.");
					}
				}
			} catch (Exception e) {
				logger__.warn("Failed to remove inactive users.", e);
			}
		}
		
	}
	
}
