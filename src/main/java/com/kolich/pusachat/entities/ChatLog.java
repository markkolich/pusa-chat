package com.kolich.pusachat.entities;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.ForwardingQueue;
import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.events.Message;
import com.kolich.pusachat.entities.events.PusaChatEvent;

public final class ChatLog extends PusaChatEntity {
	
	/**
	 * The default maximum number of message events to store per chat.
	 */
	private static final int DEFAULT_MAX_MESSAGE_EVENT_LOG_SIZE = 50;

	@SerializedName("log")
	private Queue<PusaChatEvent> messages_;
	
	public ChatLog(int maxMessages) {
		messages_ = new BoundedQueue<PusaChatEvent>(maxMessages);
	}
	
	public ChatLog() {
		this(DEFAULT_MAX_MESSAGE_EVENT_LOG_SIZE);
	}
	
	public synchronized ChatLog addMessage(PusaChatEvent event) {
		messages_.add(event);		
		return this;
	}
	
	public synchronized ChatLog deleteMessage(PusaChatEvent event) {
		messages_.remove(event);
		return this;
	}
		
	public synchronized List<Message> getMessages() {
		return Arrays.asList(messages_.toArray(new Message[]{}));
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
	
	private static final class BoundedQueue<T> extends ForwardingQueue<T> {
		
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
		
	}

}
