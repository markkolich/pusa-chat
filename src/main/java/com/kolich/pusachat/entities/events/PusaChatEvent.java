package com.kolich.pusachat.entities.events;

import java.util.Date;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.PusaChatEntity;

public abstract class PusaChatEvent extends PusaChatEntity
	implements Comparable<PusaChatEvent> {
	
	public static enum PusaChatEventType {
		
		/**
		 * A message was sent.
		 */
		MESSAGE,
		
		/**
		 * A deleted message.
		 */
		DELETE,
		
		/**
		 * A client status update was sent (inactive, active,
		 * and dead clients).
		 */
		CLIENT_STATUS,
		
		/**
		 * A client joined the room.
		 */
		CLIENT_JOINED,
		
		/**
		 * Someone is typing.
		 */
		TYPING,
		
		/**
		 * A no-op.
		 */
		NOOP
		
	};
	
	@SerializedName("id")
	private final UUID id_;
	
	@SerializedName("when")
	private final Date when_;
	
	@SerializedName("type")
	private final PusaChatEventType type_;
	
	public PusaChatEvent(PusaChatEventType type) {
		id_ = UUID.randomUUID();
		when_ = new Date();
		type_ = type;
	}
	
	public UUID getId() {
		return id_;
	}
	
	public Date getWhen() {
		return when_;
	}
	
	public PusaChatEventType getType() {
		return type_;
	}
	
	@Override
	public int compareTo(PusaChatEvent m) {
		return m.getWhen().compareTo(getWhen());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id_ == null) ? 0 : id_.hashCode());
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
		PusaChatEvent other = (PusaChatEvent) obj;
		if (id_ == null) {
			if (other.id_ != null)
				return false;
		} else if (!id_.equals(other.id_))
			return false;
		return true;
	}
	
}