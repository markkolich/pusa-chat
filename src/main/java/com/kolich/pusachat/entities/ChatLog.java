package com.kolich.pusachat.entities;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.kolich.pusachat.entities.events.Message;

public final class ChatLog extends PusaChatEntity {
	
	/**
	 * The default maximum number of message events to store per chat.
	 */
	private static final int DEFAULT_MAX_MESSAGE_EVENT_LOG_SIZE = 50;

	@SerializedName("log")
	private List<Message> messages_;

	/**
	 * The maximum number of messages allowed in this log.
	 */
	private transient int maxMessages_;

	/**
	 * An internal tag used to track the K,V store "version"
	 * of this log as fetched from the store.
	 */
	private transient String eTag_;
	
	public ChatLog(int maxMessages, String eTag) {
		messages_ = new LinkedList<Message>();
		maxMessages_ = maxMessages;
		eTag_ = eTag;
	}
	
	public ChatLog(String eTag) {
		this(DEFAULT_MAX_MESSAGE_EVENT_LOG_SIZE, eTag);
	}
	
	public ChatLog() {
		this(null);
	}
	
	public synchronized ChatLog addMessage(Message message) {
		messages_.add(message);
		// If the new size is greater than the maximum number of allowed
		// messages in this log, remove the first entity.
		if(messages_.size() > maxMessages_) {
			messages_.remove(0);
		}
		// Sort validation (to ensure that the list stays sorted).
		// Uses a custom Comparator inline because the messages in the log
		// should be sorted where the newest message is at the bottom.
		Collections.sort(messages_, new Comparator<Message>() {
			@Override
			public int compare(final Message m1, final Message m2) {
				// the value 0 if the argument Date is equal to this Date; a
				// value less than 0 if this Date is before the Date argument;
				// and a value greater than 0 if this Date is after the Date
				// argument.
				if(m1.getWhen().equals(m2.getWhen())) {
					return 0;
				} else if(m1.getWhen().before(m2.getWhen())) {
					return -1;
				} else {
					return 1;
				}
			}
		});
		return this;
	}
		
	public synchronized List<Message> getMessages() {
		return Collections.unmodifiableList(messages_);
	}
	
	public synchronized ChatLog clearMessages() {
		messages_.clear();
		return this;
	}
	
	public String getETag() {
		return eTag_;
	}
	
	public ChatLog setETag(String eTag) {
		eTag_ = eTag;
		return this;
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

}
