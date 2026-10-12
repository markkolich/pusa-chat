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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import pusachat.entities.events.Message;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The most recent messages in a room, held in memory and shown to clients when they join.
 * Once full, adding a message drops the oldest one.
 */
public final class ChatLog implements PusaChatEntity {

    private final int capacity_;

    private final Deque<Message> messages_;

    public ChatLog(
            final int capacity) {
        capacity_ = capacity;
        messages_ = new ArrayDeque<>(capacity);
    }

    public synchronized void addMessage(
            final Message message) {
        if (messages_.size() >= capacity_) {
            messages_.poll();
        }
        messages_.add(message);
    }

    public synchronized void deleteMessage(
            final UUID messageId) {
        messages_.removeIf(message -> message.getId().equals(messageId));
    }

    /**
     * A snapshot of the log, oldest message first.
     */
    @JsonProperty("log")
    public synchronized List<Message> getMessages() {
        return ImmutableList.copyOf(messages_);
    }

}
