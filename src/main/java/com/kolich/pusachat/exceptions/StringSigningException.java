package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class StringSigningException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;
	
	public StringSigningException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public StringSigningException(String message) {
		super(message);
	}
	
}
