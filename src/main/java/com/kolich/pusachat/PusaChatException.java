package com.kolich.pusachat;

import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import com.kolich.common.KolichCommonException;

/**
 * We declare our own exception type so that we can serve up
 * different types of responses in Spring based on the exception
 * case using a {@link SimpleMappingExceptionResolver}.  Other
 * exceptions the UI might care about shall extend
 * {@link PusaChatException}.
 * 
 * @author Mark Kolich
 *
 */
public class PusaChatException extends KolichCommonException {

	private static final long serialVersionUID = -1248851009621911296L;
	
	public PusaChatException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public PusaChatException(Throwable cause) {
		super(cause);
	}
	
	public PusaChatException(String message) {
		super(message);
	}
	
	public PusaChatException() {
		super();
	}

}
