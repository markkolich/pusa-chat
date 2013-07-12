package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class BadChatTokenException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;
	
	public BadChatTokenException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public BadChatTokenException(String message) {
		super(message);
	}
	
}
