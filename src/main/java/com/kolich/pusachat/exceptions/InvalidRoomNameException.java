package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class InvalidRoomNameException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;
	
	public InvalidRoomNameException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidRoomNameException(String message) {
		super(message);
	}
	
}
