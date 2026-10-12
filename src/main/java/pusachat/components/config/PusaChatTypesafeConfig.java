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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import curacao.annotations.Component;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A {@link PusaChatConfig} implementation backed by the Typesafe (a.k.a., Lightbend)
 * configuration library.
 */
@Component
public final class PusaChatTypesafeConfig implements PusaChatConfig {

    private final boolean devMode_;
    private final Duration removeInactiveUsersAfter_;
    private final int maxCachedMessagesPerRoom_;
    private final Duration longPollTimeout_;

    public PusaChatTypesafeConfig() {
        final Config config = ConfigFactory.load().getConfig(PUSACHAT_CONFIG_PATH);
        devMode_ = config.getBoolean(DEV_MODE_PROP);
        removeInactiveUsersAfter_ = config.getDuration(REMOVE_INACTIVE_USERS_AFTER_PROP);
        maxCachedMessagesPerRoom_ = config.getInt(MAX_CACHED_MESSAGES_PER_ROOM_PROP);
        longPollTimeout_ = config.getDuration(LONG_POLL_TIMEOUT_PROP);

        checkArgument(removeInactiveUsersAfter_.isPositive(),
                PUSACHAT_CONFIG_PATH + "." + REMOVE_INACTIVE_USERS_AFTER_PROP + " must be greater than zero.");
        checkArgument(maxCachedMessagesPerRoom_ > 0,
                PUSACHAT_CONFIG_PATH + "." + MAX_CACHED_MESSAGES_PER_ROOM_PROP + " must be greater than zero.");
        checkArgument(longPollTimeout_.isPositive(),
                PUSACHAT_CONFIG_PATH + "." + LONG_POLL_TIMEOUT_PROP + " must be greater than zero.");
    }

    @Override
    public boolean isDevMode() {
        return devMode_;
    }

    @Override
    public Duration getRemoveInactiveUsersAfter() {
        return removeInactiveUsersAfter_;
    }

    @Override
    public int getMaxCachedMessagesPerRoom() {
        return maxCachedMessagesPerRoom_;
    }

    @Override
    public Duration getLongPollTimeout() {
        return longPollTimeout_;
    }

}
