package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.NOOP;

public final class NoOp extends PusaChatEvent {
		
	public NoOp() {
		super(NOOP);
	}
	
}