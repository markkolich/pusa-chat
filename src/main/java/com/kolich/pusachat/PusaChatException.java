/**
 * Copyright (c) 2013 Mark S. Kolich
 * http://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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