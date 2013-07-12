package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.DELETE;

import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public final class Delete extends PusaChatEvent {
	
	/**
	 * This is the "query parameter" used by the client to send a
	 * delete message request to the application.
	 */
	public static final String MESSAGE_ID_DELETE_QUERY_PARAM = "messageId";
	
	/**
	 * The {@link UUID} of the deleted message.
	 */
	@SerializedName("delete")
	private final UUID messageId_;
	
	public Delete(UUID messageId) {
		super(DELETE);
		messageId_ = messageId;
	}
	
	// For GSON
	protected Delete() {
		this(null);
	}
	
	public UUID getMessageId() {
		return messageId_;
	}
	
}