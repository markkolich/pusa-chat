package com.kolich.pusachat.spring.beans;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;

import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.entities.ChatRoom;
import com.kolich.pusachat.exceptions.RoomNotFoundException;

public final class ChatRooms implements InitializingBean {
	
	private Map<UUID, ChatRoom> chatRooms_;
	
	private Map<String, UUID> keysToRooms_;
	private Map<UUID, String> roomsToKeys_;
	
	private KolichStringSigner signer_;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		chatRooms_ = new ConcurrentHashMap<UUID, ChatRoom>();
		// Reverse maps.
		keysToRooms_ = new ConcurrentHashMap<String, UUID>();
		roomsToKeys_ = new ConcurrentHashMap<UUID, String>();
	}
	
	public synchronized ChatRoom getRoom(final UUID roomId) {
		final ChatRoom cr = chatRooms_.get(roomId);
		if(cr == null) {
			throw new RoomNotFoundException("Found no room for chat UUID: " +
				roomId);
		}
		return cr;
	}
	
	public synchronized ChatRoom getRoom(final String roomId) {
		try {
			return getRoom(UUID.fromString(roomId));
		} catch (IllegalArgumentException e) {
			throw new RoomNotFoundException("Malformed chat room UUID: " +
				roomId, e);
		}
	}
	
	public synchronized ChatRoom newRoom(final String roomName) {
		final UUID roomId = UUID.randomUUID();
		final ChatRoom cr = new ChatRoom(roomName,
			// The room's UUID.
			roomId,
			// Digitally signed room UUID (a.k.a., room token).
			signer_.sign(roomId.toString()));
		chatRooms_.put(cr.getId(), cr);
		keysToRooms_.put(roomName, cr.getId());
		roomsToKeys_.put(cr.getId(), roomName);
		return cr;
	}
	
	public synchronized ChatRoom getRoomFromKey(final String roomName,
		final boolean create) {
		ChatRoom cr = null;
		UUID id = null;
		if((id = keysToRooms_.get(roomName)) != null) {
			try {
				cr = getRoom(id);
			} catch (RoomNotFoundException nfe) { }
		}
		// If we were not asked to create one, and nothing was found, bail.
		if(cr == null && !create) {
			throw new RoomNotFoundException("Room not found: " + roomName);
		} else if(cr == null && create) {
			cr = newRoom(roomName);
		}
		return cr;
	}
	
	public synchronized Set<UUID> getAllRooms() {
		return Collections.unmodifiableSet(chatRooms_.keySet());
	}
		
	public synchronized void deleteRoom(final UUID roomId) {
		chatRooms_.remove(roomId);
		final String roomName;
		if((roomName = roomsToKeys_.remove(roomId)) != null) {
			keysToRooms_.remove(roomName);
		}
	}
	
	public void setSigner(KolichStringSigner signer) {
		signer_ = signer;
	}
	
}
