package com.kolich.pusachat.entities;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.events.PusaChatEvent;

public final class PusaChatSession extends PusaChatEntity {
	
	/**
	 * The room {@link UUID} to which this session belongs.
	 */
	@SerializedName("room_id")
	private final UUID roomId_;
	
	/**
	 * The client {@link UUID} to which this session belongs.
	 */
	@SerializedName("client_id")
	private final UUID clientId_;
	
	/**
	 * A token is a unique, digitally signed, identifier that identifies
	 * this session, owned by this client, in this room.  The point is that
	 * you can't guess this and masquerade as another client; pretending to
	 * be a registered client that you're not, hence waiting for or posting
	 * events on behalf of some other user.  Only registered clients have
	 * access to their own token, and you only get a token once you register.
	 */
	@SerializedName("token")
	private final String token_;
	
	/**
	 * When this session was last accessed.
	 */
	@SerializedName("last_accessed")
	private final Date lastAccessed_;
	
	/**
	 * A queue of events belonging to this session, of this client,
	 * in this room.
	 */
	private transient final BlockingQueue<PusaChatEvent> eventQueue_;
	
	public PusaChatSession(UUID roomId, UUID clientId, String token) {
		roomId_ = roomId;
		clientId_ = clientId;
		token_ = token;
		lastAccessed_ = new Date();
		eventQueue_ = new LinkedBlockingQueue<PusaChatEvent>();
	}
	
	// For GSON
	protected PusaChatSession() {
		this(randomUUID(), randomUUID(), null);
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
	
	public synchronized Date getLastAccessed() {
		return lastAccessed_;
	}
	
	/**
	 * Convenient method for setting the last accessed date to "now".
	 */
	public synchronized PusaChatSession wasAccessed() {
		lastAccessed_.setTime(currentTimeMillis());
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((clientId_ == null) ? 0 : clientId_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PusaChatSession other = (PusaChatSession) obj;
		if (clientId_ == null) {
			if (other.clientId_ != null)
				return false;
		} else if (!clientId_.equals(other.clientId_))
			return false;
		return true;
	}	

}
