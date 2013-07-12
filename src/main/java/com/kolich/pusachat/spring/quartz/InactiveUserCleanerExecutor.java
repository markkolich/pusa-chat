package com.kolich.pusachat.spring.quartz;

import static java.lang.System.currentTimeMillis;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kolich.pusachat.entities.ChatRoom;
import com.kolich.pusachat.entities.events.PusaChatEvent;
import com.kolich.pusachat.exceptions.RoomNotFoundException;
import com.kolich.pusachat.spring.beans.ChatRooms;

public class InactiveUserCleanerExecutor implements Runnable {
	
	private static final Logger logger__ =
		LoggerFactory.getLogger(InactiveUserCleanerExecutor.class);
	
	private static final long DEFAULT_ONE_MINUTE_IN_MS = 60000L;
	
	/**
	 * How "old" inactive users can be before they are considered
	 * absent from the room.
	 */
	private long expiry_ = DEFAULT_ONE_MINUTE_IN_MS;
	
	private ChatRooms rooms_;

	@Override
	public void run() {
		try {
			// Destroy all users who haven't ping'ed the room within X-seconds.
			final Date when = new Date(currentTimeMillis() - expiry_);
			// List all rooms, and cleanup each one.
			final Set<UUID> rooms = rooms_.getAllRooms();
			for(final UUID roomId : rooms) {
				try {
					final ChatRoom room = rooms_.getRoom(roomId);
					// Remove all clients from the room who haven't pinged
					// us since "when", the expiry.
					if(room.removeInactiveClients(when)) {
						// If we did remove a client, then let everyone in
						// the room know about the event.
						room.postEvents(Arrays.asList(new PusaChatEvent[]{
							room.getClientStatus(),
							room.getTyping()
						}));
					}
				} catch (RoomNotFoundException rnfe) {
					logger__.warn("Room (" + roomId.toString() + ") was " +
						"in rooms list, but seems to have disappeared; " +
							"can't remove inactive users.");
				}
			}
		} catch (Exception e) {
			logger__.warn("Failed to remove inactive users!", e);
		}
	}
	
	public void setChatRooms(ChatRooms chatRooms) {
		rooms_ = chatRooms;
	}
	
	public void setExpiry(long expiry) {
		expiry_ = expiry;
	}

}
