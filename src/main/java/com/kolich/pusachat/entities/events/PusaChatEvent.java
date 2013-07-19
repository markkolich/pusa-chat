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
	
	public final UUID getId() {
		return id_;
	}
	
	public final Date getWhen() {
		return when_;
	}
	
	public final PusaChatEventType getType() {
		return type_;
	}
	
	@Override
	public final int compareTo(PusaChatEvent m) {
		return m.getWhen().compareTo(getWhen());
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id_ == null) ? 0 : id_.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
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