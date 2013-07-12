package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.TYPING;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public final class Typing extends PusaChatEvent {
	
	@SerializedName("typing")
	private final Set<UUID> whosTyping_;
	
	public Typing(Set<UUID> whosTyping) {
		super(TYPING);
		whosTyping_ = whosTyping;
	}
	
	// For GSON
	protected Typing() {
		this(null);
	}
	
	public Set<UUID> getWhosTyping() {
		return Collections.unmodifiableSet(whosTyping_);
	}
	
}