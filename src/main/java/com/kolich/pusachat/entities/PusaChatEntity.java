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

package com.kolich.pusachat.entities;

import static com.kolich.common.date.ISO8601DateFormat.getPrimaryFormat;
import static java.util.TimeZone.getTimeZone;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kolich.common.entities.KolichCommonEntity;
import com.kolich.common.entities.gson.KolichDefaultDateTypeAdapter;

public abstract class PusaChatEntity extends KolichCommonEntity {
	
	private static final DateFormat iso8601Format__;
	static {
		iso8601Format__ = new SimpleDateFormat(getPrimaryFormat());
		iso8601Format__.setTimeZone(getTimeZone("GMT"));
	}

	public static final GsonBuilder getNewPusaChatGsonBuilder() {
		return getDefaultGsonBuilder()
			.registerTypeAdapter(new TypeToken<Date>(){}.getType(),
				new KolichDefaultDateTypeAdapter(iso8601Format__));
	}
	
	public static final Gson getNewPusaChatGsonInstance() {
		return getNewPusaChatGsonBuilder().create();
	}
	
	@Override
	public final String toString() {
		return getNewPusaChatGsonInstance().toJson(this);
	}
	
	@Override
	public final byte[] getBytes() {
		return getBytesUtf8(toString());
	}
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object o);
	
}