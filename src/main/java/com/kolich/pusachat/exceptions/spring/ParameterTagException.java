package com.kolich.pusachat.exceptions.spring;

import com.kolich.pusachat.PusaChatException;

public final class ParameterTagException extends PusaChatException {

	private static final long serialVersionUID = 7142655249790444658L;

	public ParameterTagException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ParameterTagException(String message) {
		super(message);
	}
	
	public ParameterTagException() {
		super();
	}

}