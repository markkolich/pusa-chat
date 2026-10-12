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

package pusachat.components.config;

import java.time.Duration;

/**
 * Application configuration: everything under the {@code pusachat} path of the HOCON config.
 */
public interface PusaChatConfig {

    String PUSACHAT_CONFIG_PATH = "pusa-chat";

    String DEV_MODE_PROP = "dev-mode";
    String CONTEXT_PATH_PROP = "context-path";
    String REMOVE_INACTIVE_USERS_AFTER_PROP = "remove-inactive-users-after";
    String MAX_CACHED_MESSAGES_PER_ROOM_PROP = "max-cached-messages-per-room";
    String LONG_POLL_TIMEOUT_PROP = "long-poll-timeout";

    boolean isDevMode();

    /**
     * Clients that haven't polled for events in this long are removed from their room.
     */
    Duration getRemoveInactiveUsersAfter();

    int getMaxCachedMessagesPerRoom();

    /**
     * How long a request for a client's next event waits before returning a NOOP.
     */
    Duration getLongPollTimeout();

}
