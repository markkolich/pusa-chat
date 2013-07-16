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

package com.kolich.pusachat.spring;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.kolich.spring.beans.KolichWebAppProperties;

public final class PusaChatProperties extends KolichWebAppProperties {
	
	private static final String APP_VERSION_PROPERTY = "pusachat.app.version";
	private static final String APP_MODE_PROPERTY = "pusachat.app.mode";
	private static final String CONTEXT_PATH_PROPERTY = "pusachat.context.path";
	
	private static final String REMOVE_INACTIVE_USERS_AFTER_PROPERTY =
		"pusachat.remove-inactive-users-after.ms";
	private static final String MAX_CACHED_MESSAGES_PER_ROOM =
		"pusachat.max-cached-messages-per-room";
	
	private static final String MODE_PRODUCTION = "production";

	@Override
	public void afterPropertiesSet() throws Exception {
		checkNotNull(getAppVersion(), "Application version cannot be null.");
		checkNotNull(getAppMode(), "Application mode cannot be null.");
		checkNotNull(getContextPath(), "Context path cannot be null.");
		checkNotNull(getRemoveInactiveAfter(), "Remove inactive users after " +
			"N-milliseconds cannot be null.");
		checkArgument(getRemoveInactiveAfter() > 0L, "Removing inactive " +
			"users after N-milliseconds must be greater than zero.");
		checkNotNull(getMaxCachedMessagePerRoom(), "Max cached chat messages " +
			"per room cannot be null.");
		checkArgument(getMaxCachedMessagePerRoom() > 0, "Max cached chat " +
			"messages per room must be greater than zero.");
	}
	
	public final String getAppVersion() {
		return (String)getProperty(APP_VERSION_PROPERTY);
	}
	
	public final String getAppMode() {
		return (String)getProperty(APP_MODE_PROPERTY);
	}
	
	public final boolean isProductionMode() {
		return MODE_PRODUCTION.equals(getAppMode());
	}
	
	public final String getContextPath() {
		return (String)getProperty(CONTEXT_PATH_PROPERTY);
	}
	
	public final Long getRemoveInactiveAfter() {
		return Long.parseLong((String)getProperty(
			REMOVE_INACTIVE_USERS_AFTER_PROPERTY));
	}
	
	public final Integer getMaxCachedMessagePerRoom() {
		return Integer.parseInt((String)getProperty(
			MAX_CACHED_MESSAGES_PER_ROOM));		
	}

}