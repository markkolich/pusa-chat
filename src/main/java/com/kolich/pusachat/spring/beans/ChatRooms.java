package com.kolich.pusachat.spring.beans;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

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
		final long removeInactiveUsersAfter =
			properties_.getRemoveInactiveAfter();
		// Setup a new thread factory builder.
		executor_ = newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("inactive-user-cleaner-%d")
				.build());
		// Schedule a new cleaner at a "fixed" interval.
		executor_.scheduleAtFixedRate(
			new InactiveUserCleanerExecutor(this, removeInactiveUsersAfter),
			0L,  // initial delay
			removeInactiveUsersAfter, // repeat every
			SECONDS); // units
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
			signer_.sign(roomId.toString()));
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
