package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.CLIENT_JOINED;

import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public final class ClientJoined extends PusaChatEvent {
	
	@SerializedName("client_id")
	private final UUID clientId_;
		
	public ClientJoined(UUID clientId) {
		super(CLIENT_JOINED);
		clientId_ = clientId;
	}

	public UUID getClientId() {
		return clientId_;
	}
	
}