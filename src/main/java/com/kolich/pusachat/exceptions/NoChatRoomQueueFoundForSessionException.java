package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class NoChatRoomQueueFoundForSessionException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;

	public NoChatRoomQueueFoundForSessionException(String sessionId) {
		super("Session was not registered: " + sessionId);
	}
	
}
