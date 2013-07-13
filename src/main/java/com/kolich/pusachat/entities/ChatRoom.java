package com.kolich.pusachat.entities;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.events.ClientStatus;
import com.kolich.pusachat.entities.events.PusaChatEvent;
import com.kolich.pusachat.entities.events.Typing;
import com.kolich.pusachat.exceptions.ClientNotRegisteredWithChatRoomException;

public final class ChatRoom extends PusaChatEntity {
	
	private static final Logger logger__ =
		LoggerFactory.getLogger(ChatRoom.class);
		
	private static final Pattern VALID_ROOM_NAME_PATTERN =
    	compile("\\A[a-zA-Z0-9_\\-]{1,36}\\Z", CASE_INSENSITIVE);

	/**
	 * This room's UUID.
	 * Not serialized by GSON.
	 */
	private final transient UUID roomId_;
	
	/**
	 * A digitally signed token used to securely
	 * encapsulate the room UUID.
	 */
	@SerializedName("token")
	private final String token_;
	
	@SerializedName("created_on")
	private final Date createdOn_;
	
	@SerializedName("name")
	private final String name_;
	
	/**
	 * A transient field is not serialized by GSON.  This is a
	 * map that maintains a list of client ID's mapped to their
	 * corresponding {@link BlockingQueue}.
	 */
	private final transient Map<UUID, PusaChatSession> sessions_;
	
	/**
	 * A list of clients who are actively "typing".
	 * Not serialized by GSON.
	 */
	private final transient Set<UUID> typing_;
	
	/**
	 * A list of inactive clients.
	 * Not serialized by GSON.
	 */
	private final transient Set<UUID> inactive_;
	
	/**
	 * A log of chat messages in this room.  Stored only
	 * in memory.  Not serialized by GSON.
	 */
	private final transient ChatLog log_;
	
	public ChatRoom(String name, UUID roomId, String token) {
		roomId_ = roomId;
		token_ = token;
		createdOn_ = new Date();
		name_ = name;
		sessions_ = new ConcurrentHashMap<UUID, PusaChatSession>();
		typing_ = new HashSet<UUID>();
		inactive_ = new HashSet<UUID>();
		log_ = new ChatLog();
	}
	
	public ChatRoom(String name, String token) {
		this(name, UUID.randomUUID(), token);
	}
	
	// For GSON
	protected ChatRoom() {
		this(null, null);
	}

	public UUID getId() {
		return roomId_;
	}
	
	public String getToken() {
		return token_;
	}
	
	public Date getCreatedOn() {
		return createdOn_;
	}
	
	public String getName() {
		return name_;
	}
	
	public synchronized PusaChatSession registerClient(final UUID clientId,
		final String token) {
		PusaChatSession session = null;
		if((session = sessions_.get(clientId)) == null) {
			session = new PusaChatSession(getId(), clientId, token);
			sessions_.put(clientId, session);
		}
		return session;
	}
		
	public synchronized PusaChatSession getSessionByClientId(final UUID clientId) {
		PusaChatSession session = null;
		if((session = sessions_.get(clientId)) == null) {
			// If there was no session tied to this room, then throw an
			// exception since this indicates that the client has not yet
			// registered with this room.  Out of spec.
			throw new ClientNotRegisteredWithChatRoomException(
				clientId.toString());
		}
		return session;
	}
	
	public synchronized BlockingQueue<PusaChatEvent> getQueueByClientId(
		final UUID clientId) {
		final PusaChatSession session = getSessionByClientId(clientId);
		return (session != null) ? session.getEventQueue() : null;
	}
			
	public synchronized ChatRoom postEvent(final PusaChatEvent event) {
		// For each queue associated with this room, post the event to it.
		final Iterator<UUID> it = sessions_.keySet().iterator();
		while(it.hasNext()) {
			final UUID clientId = it.next();
			try {
				// Get the queue, should exist, do not create it if not.
				getQueueByClientId(clientId).add(event);
			} catch (Exception e) {
				logger__.debug("Failed to post event to client queue " +
					"(clientId=" + clientId + ")", e);
			}
		}
		return this;
	}
	
	public synchronized void postEvents(final List<PusaChatEvent> events) {
		for(final PusaChatEvent event : events) {
			postEvent(event);
		}
	}
	
	public synchronized ClientStatus getClientStatus() {
		return new ClientStatus(sessions_.size(), inactive_.size());
	}
	
	public synchronized Typing getTyping() {
		return new Typing(typing_);
	}
	
	public synchronized Typing updateTyping(final UUID clientId,
		final boolean isTyping) {
		if(isTyping) {
			typing_.add(clientId);
		} else {
			typing_.remove(clientId);
		}
		return getTyping();
	}
	
	public synchronized Typing updateTyping(final String clientId,
		final boolean isTyping) {
		return updateTyping(UUID.fromString(clientId), isTyping);
	}
	
	public synchronized ClientStatus updateInactivity(final UUID clientId,
		final boolean isActive) {
		if(!isActive) {
			inactive_.add(clientId);
		} else {
			inactive_.remove(clientId);
		}
		return getClientStatus();
	}
	
	public synchronized ClientStatus updateInactivity(final String clientId,
		final boolean isActive) {
		return updateInactivity(UUID.fromString(clientId), isActive);
	}
	
	public synchronized boolean removeInactiveClients(final Date expiry) {
		boolean removed = false;
		final Iterator<UUID> it = sessions_.keySet().iterator();
		while(it.hasNext()) {
			final UUID clientId = it.next();
			final PusaChatSession session = getSessionByClientId(clientId);
			if(session.getLastAccessed().before(expiry)) {
				logger__.info("Client left room (clientId=" + clientId +
					", roomId=" + roomId_ + ", lastAccessed=" +
					session.getLastAccessed() + ")");
				sessions_.remove(clientId);
				typing_.remove(clientId);
				inactive_.remove(clientId);
				removed = true;
			}
		}
		return removed;
	}
	
	public synchronized List<UUID> getActiveClients() {
		final List<UUID> activeClients = new ArrayList<UUID>();
		final Iterator<UUID> it = sessions_.keySet().iterator();
		while(it.hasNext()) {
			// Assumption that if the client is on the list of sessions
			// we're maintaining, then they're "active".
			activeClients.add(it.next());
		}
		return activeClients;
	}
	
	public synchronized ChatLog getChatLog() {
		return log_;
	}
		
	public static final boolean isValidRoomName(final String roomName) {
		return VALID_ROOM_NAME_PATTERN.matcher(roomName).matches();
	}
	
	// Straight from Eclipse
	// Only compares the room UUID
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((roomId_ == null) ? 0 : roomId_.hashCode());
		return result;
	}

	// Straight from Eclipse
	// Only compares the room UUID
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatRoom other = (ChatRoom) obj;
		if (roomId_ == null) {
			if (other.roomId_ != null)
				return false;
		} else if (!roomId_.equals(other.roomId_))
			return false;
		return true;
	}
	
}
