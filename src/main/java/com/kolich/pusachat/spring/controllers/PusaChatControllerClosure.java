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

package com.kolich.pusachat.spring.controllers;

import org.slf4j.Logger;

import com.kolich.pusachat.PusaChatException;
import com.kolich.pusachat.exceptions.BadChatTokenException;
import com.kolich.pusachat.exceptions.ClientNotRegisteredWithChatRoomException;
import com.kolich.pusachat.exceptions.InvalidRoomNameException;
import com.kolich.pusachat.exceptions.NoChatRoomQueueFoundForSessionException;
import com.kolich.pusachat.exceptions.RoomNotFoundException;
import com.kolich.spring.controllers.KolichControllerClosure;

public abstract class PusaChatControllerClosure<T>
	extends KolichControllerClosure<T> {

	public PusaChatControllerClosure(String comment, Logger logger) {
		super(comment, logger);
	}
	
	@Override
	public final T execute() {
		try {
			logger_.debug("Entering: " + comment_);
			return doit();
		} catch (RoomNotFoundException e) {
			logger_.debug("Room not found: " + comment_, e);
			throw e;
		} catch (InvalidRoomNameException e) {
			logger_.debug("Invalid room name: " + comment_, e);
			throw e;
		} catch(NoChatRoomQueueFoundForSessionException e) {
			logger_.debug("No queue found for session: " + comment_, e);
			throw e;
		} catch(ClientNotRegisteredWithChatRoomException e) {
			logger_.debug("Client not registered with room: " + comment_, e);
			throw e;
		} catch (BadChatTokenException e) {
			logger_.debug("Invalid chat token: " + comment_, e);
			throw e;
		} catch (IllegalArgumentException e) {
			logger_.debug(comment_, e);
			throw e;
		} catch (Exception e) {
			logger_.error(comment_, e);
			throw new PusaChatException(e);
		}
	}

}
