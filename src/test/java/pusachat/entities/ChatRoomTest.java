/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
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

package pusachat.entities;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pusachat.entities.events.Delete;
import pusachat.entities.events.Message;
import pusachat.entities.events.PusaChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ChatRoomTest {

    private static final int MESSAGES = 1000;

    /**
     * Several threads, like the real pool under load, so events could be delivered out of order
     * if the room didn't run its own events one at a time.
     */
    private final ExecutorService pool_ = Executors.newFixedThreadPool(8);

    private final ChatRoom room_ = new ChatRoom("room", UUID.randomUUID(), "token", MESSAGES,
            MoreExecutors.newSequentialExecutor(pool_));

    @AfterEach
    public void shutdownPool() {
        pool_.shutdownNow();
    }

    @Test
    public void everyClientGetsEventsInTheOrderTheyWerePosted() throws Exception {
        final PusaChatSession alice = room_.registerClient(UUID.randomUUID(), "alice");
        final PusaChatSession bob = room_.registerClient(UUID.randomUUID(), "bob");

        // Messages, with every other one deleted right after it's posted.
        final List<UUID> posted = new ArrayList<>();
        final List<Message> kept = new ArrayList<>();
        for (int i = 0; i < MESSAGES; i++) {
            final Message message = new Message.Builder()
                    .setFrom(alice.getClientId().toString())
                    .setHtml("message " + i)
                    .build();
            room_.postEvent(message);
            posted.add(message.getId());
            if (i % 2 == 0) {
                final Delete delete = new Delete.Builder()
                        .setMessageId(message.getId())
                        .build();
                room_.postEvent(delete);
                posted.add(delete.getId());
            } else {
                kept.add(message);
            }
        }

        assertEquals(posted, receive(alice, posted.size()));
        assertEquals(posted, receive(bob, posted.size()));

        // The log is updated after each event is delivered: let the last one finish.
        pool_.shutdown();
        assertTrue(pool_.awaitTermination(5L, TimeUnit.SECONDS));
        assertEquals(kept, room_.getChatLog().getMessages());
    }

    private static List<UUID> receive(
            final PusaChatSession session,
            final int count) throws InterruptedException {
        final List<UUID> received = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final PusaChatEvent event = session.getEventQueue().poll(5L, TimeUnit.SECONDS);
            assertNotNull(event, "Timed out waiting for event " + i);
            received.add(event.getId());
        }
        return received;
    }

}
