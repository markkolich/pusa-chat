package com.kolich.pusachat.exceptions;

import com.kolich.pusachat.PusaChatException;

public class InvalidApiExtensionException extends PusaChatException {

	private static final long serialVersionUID = -7863924412834952274L;

	public InvalidApiExtensionException(String resource) {
		super("Invalid API extension: " + resource);
	}
	
}
