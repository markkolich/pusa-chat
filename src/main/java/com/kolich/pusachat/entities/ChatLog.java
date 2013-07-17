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

package com.kolich.pusachat.entities;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import com.google.common.collect.ForwardingQueue;
import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.events.Delete;
import com.kolich.pusachat.entities.events.Message;
import com.kolich.pusachat.entities.events.PusaChatEvent;

public final class ChatLog extends PusaChatEntity {
	
	@SerializedName("log")
	private Queue<PusaChatEvent> messages_;
	
	public ChatLog(int maxMessages) {
		messages_ = new BoundedQueue<PusaChatEvent>(maxMessages);
	}
	
	// For GSON
	public ChatLog() {
		this(-1);
	}
	
	public synchronized ChatLog addMessage(Message message) {
		messages_.add(message);		
		return this;
	}
	
	public synchronized ChatLog deleteMessage(Delete deleted) {
		((BoundedQueue<PusaChatEvent>)messages_).removeEventById(
			deleted.getMessageId());
		return this;
	}
	
	public synchronized List<Message> getMessages() {
		// Something about this toArray(), asList(), unmodifiableList() feels
		// a bit wrong.  It's probably not right.  Should perhaps revisit this
		// at some point.
		return unmodifiableList(asList(messages_.toArray(new Message[]{})));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messages_ == null) ? 0 : messages_.hashCode());
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
		ChatLog other = (ChatLog) obj;
		if (messages_ == null) {
			if (other.messages_ != null)
				return false;
		} else if (!messages_.equals(other.messages_))
			return false;
		return true;
	}
	
	/**
	 * A bounded, or fixed size, queue which automatically drops elements
	 * from the end of the queue if its size grows beyond a set capacity.
	 * Note that access to the underlying queue instance isn't synchronized,
	 * as we're relying on the consuming class "ChatLog" to handle that for
	 * us.
	 */
	private static final class BoundedQueue<T extends PusaChatEvent> 
		extends ForwardingQueue<T> {
		
		private final Queue<T> delegate_;
		private final int capacity_;
		
		public BoundedQueue(int capacity) {
			capacity_ = capacity;
			delegate_ = new ArrayDeque<T>(capacity_);
		}

		@Override
		protected Queue<T> delegate() {
			return delegate_;
		}
		
		@Override
		public boolean add(final T t) {
			if(size() >= capacity_) {
				delegate_.poll();
			}
			return delegate_.add(t);
		}
		
		@Override
		public boolean addAll(final Collection<? extends T> collection) {
			return standardAddAll(collection);
		}
		
		@Override
		public boolean offer(final T t) {
			return standardOffer(t);
		}
		
		public boolean removeEventById(final UUID id) {
			boolean removed = false;
			for(final T t : delegate_) {
				if(t.getId().equals(id)) {
					removed = delegate().remove(t);
					break;
				}
			}
			return removed;
		}

	}

}