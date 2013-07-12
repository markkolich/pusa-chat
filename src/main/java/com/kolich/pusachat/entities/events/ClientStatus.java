package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.CLIENT_STATUS;

import com.google.gson.annotations.SerializedName;

public final class ClientStatus extends PusaChatEvent {
	
	@SerializedName("active")
	private final int activeClients_;
	
	@SerializedName("inactive")
	private final int inactiveClients_;
	
	public ClientStatus(int activeClients, int inactiveClients) {
		super(CLIENT_STATUS);
		activeClients_ = activeClients;
		inactiveClients_ = inactiveClients;
	}

	public int getActiveClients() {
		return activeClients_;
	}

	public int getInactiveClients() {
		return inactiveClients_;
	}
	
}