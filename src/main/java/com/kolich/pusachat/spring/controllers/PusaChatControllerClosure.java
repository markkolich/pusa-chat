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
