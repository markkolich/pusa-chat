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

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.kolich.common.date.ISO8601DateFormat;
import com.kolich.common.entities.KolichCommonEntity;

public abstract class PusaChatEntity extends KolichCommonEntity {
	
	public static final GsonBuilder getNewPusaChatGsonBuilder() {
		return getDefaultGsonBuilder()
			.registerTypeAdapter(new TypeToken<Date>(){}.getType(),
				new PusaChatEntityDateTypeAdapter());
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
	
	private static final class PusaChatEntityDateTypeAdapter 
		implements JsonSerializer<Date>, JsonDeserializer<Date> {
		
		private final DateFormat iso8601Format_;
	
	    private PusaChatEntityDateTypeAdapter() {
	    	iso8601Format_ = ISO8601DateFormat.getNewInstance();
	    }
	
	    @Override
		public JsonElement serialize(Date src, Type typeOfSrc,
			JsonSerializationContext context) {
	    	synchronized (iso8601Format_) {
	    		String dateFormatAsString = iso8601Format_.format(src);
	    		return new JsonPrimitive(dateFormatAsString);
	    	}
	    }
	
	    @Override
		public Date deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
	    	if (!(json instanceof JsonPrimitive)) {
	    		throw new JsonParseException("The date should be a string value.");
	    	}
	    	Date parsed = null;
	    	try {
	    		synchronized(iso8601Format_) {
    				parsed = iso8601Format_.parse(json.getAsString());
    			}
	    	} catch (ParseException e) {
	    		throw new JsonSyntaxException(e);
	    	}
	    	return parsed;
	    }
	
	}
	
}