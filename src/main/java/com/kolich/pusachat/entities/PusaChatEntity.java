package com.kolich.pusachat.entities;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

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

/**
 * Any entity should extend this abstract class, {@link PusaChatEntity}.
 * 
 * @author Mark Kolich
 *
 */
public abstract class PusaChatEntity {
	
	private static GsonBuilder builder__ = null;
	
	/**
	 * Get a fresh {@link Gson} instance created using the default
	 * {@link GsonBuilder}.
	 * @return a new pre-configured instance to serialize or deserialize
	 * a {@link PusaChatEntity}.
	 */
	public synchronized static final Gson getGsonInstance() {
		if (builder__ == null) {
			builder__ = getDefaultGsonBuilder();
		}
		return builder__.create();
	}
	
	/**
	 * Get a new, default configured, {@link GsonBuilder} instance.
	 * Pretty printing is disabled, null's are serialized, and the
	 * default date format is the primary format defined in
	 * 
	 * {@link ISO8601DateFormat}.
	 * @return
	 */
	public static final GsonBuilder getDefaultGsonBuilder() {
		return new GsonBuilder()
		// Configure Gson to serialize null fields; null fields
		// are left out of the serialized JSON by default, but we
		// want them to be there to accurately represent a real "null".
		.serializeNulls()		
		.registerTypeAdapter(new TypeToken<Date>(){}.getType(),
			new DefaultDateTypeAdapter(ISO8601DateFormat.getPrimaryFormat()));
	}
	
	/**
	 * Serialize this entity into a String; default behavior
	 * is usually to first to convert the entity into a Gson object
	 * then serializes that Gson object into a String.  Basically
	 * this method causes the entity to return a JSON serialized
	 * representation of itself.
	 */
	@Override
	public String toString() {
		return getGsonInstance().toJson(this);
	}
	
	/**
	 * Returns the entity in its default JSON object representation
	 * as a series of UTF-8 encoded bytes.
	 * @return
	 */
	public byte[] getBytes() {
		return getBytesUtf8(toString());
	}
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object o);
	
	private static class DefaultDateTypeAdapter 
		implements JsonSerializer<Date>, JsonDeserializer<Date> {
		
		private final DateFormat format_;

	    private DefaultDateTypeAdapter(final String datePattern) {
	      format_ = new SimpleDateFormat(datePattern);
	      format_.setTimeZone(new SimpleTimeZone(0, "GMT"));
	    }

	    @Override
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
	      synchronized (format_) {
	        String dateFormatAsString = format_.format(src);
	        return new JsonPrimitive(dateFormatAsString);
	      }
	    }

	    @Override
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
	        throws JsonParseException {
	      if (!(json instanceof JsonPrimitive)) {
	        throw new JsonParseException("The date should be a string value");
	      }
	      try {
	        synchronized (format_) {
	          return format_.parse(json.getAsString());
	        }
	      } catch (ParseException e) {
	        throw new JsonSyntaxException(e);
	      }
	    }

	}
	
}
