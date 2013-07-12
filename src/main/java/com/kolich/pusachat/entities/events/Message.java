package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.MESSAGE;

import com.google.gson.annotations.SerializedName;

public final class Message extends PusaChatEvent {
			
	@SerializedName("from")
	private String from_;
	
	@SerializedName("html")
	private String html_;
	
	public Message(String from, String html) {
		super(MESSAGE);
		from_ = from;
		html_ = html;
	}
	
	protected Message() {
		this(null, null);
	}

	public String getFrom() {
		return from_;
	}

	public Message setFrom(String from) {
		from_ = from;
		return this;
	}

	public String getHtml() {
		return html_;
	}

	public Message setHtml(String html) {
		html_ = html;
		return this;
	}
	
}
