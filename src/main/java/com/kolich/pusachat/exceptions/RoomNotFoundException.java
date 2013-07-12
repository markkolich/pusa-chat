package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class RoomNotFoundException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;
	
	public RoomNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public RoomNotFoundException(String message) {
		super(message);
	}
	
}
