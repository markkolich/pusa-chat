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

package com.kolich.pusachat.entities.events;

import static com.kolich.pusachat.entities.events.PusaChatEvent.PusaChatEventType.TYPING;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public final class Typing extends PusaChatEvent {
	
	@SerializedName("typing")
	private final Set<UUID> whosTyping_;
	
	public Typing(Set<UUID> whosTyping) {
		super(TYPING);
		whosTyping_ = whosTyping;
	}
	
	// For GSON
	protected Typing() {
		this(null);
	}
	
	public Set<UUID> getWhosTyping() {
		return Collections.unmodifiableSet(whosTyping_);
	}
	
}