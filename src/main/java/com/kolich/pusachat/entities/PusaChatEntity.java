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
	public String toString() {
		return getNewPusaChatGsonInstance().toJson(this);
	}
	
	@Override
	public byte[] getBytes() {
		return getBytesUtf8(toString());
	}
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object o);
	
	private static class PusaChatEntityDateTypeAdapter 
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
