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

package pusachat.entities.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static pusachat.entities.events.PusaChatEvent.EventType.MESSAGE;

public interface Message extends PusaChatEvent {

    @Override
    default EventType getType() {
        return MESSAGE;
    }

    /**
     * The client ID of the sender.
     */
    @JsonProperty("from")
    String getFrom();

    /**
     * The message as safe HTML: escaped, with links turned into anchors.
     */
    @JsonProperty("html")
    String getHtml();

    final class Builder extends AbstractPusaChatEventBuilder {

        private String from_;
        private String html_;

        public Builder setFrom(
                final String from) {
            from_ = from;
            return this;
        }

        public Builder setHtml(
                final String html) {
            html_ = html;
            return this;
        }

        public Message build() {
            checkNotNull(from_, "From cannot be null.");
            checkNotNull(html_, "HTML cannot be null.");

            return new Message() {
                @Override
                public UUID getId() {
                    return id_;
                }

                @Override
                public Instant getWhen() {
                    return when_;
                }

                @Override
                public String getFrom() {
                    return from_;
                }

                @Override
                public String getHtml() {
                    return html_;
                }
            };
        }

    }

}
