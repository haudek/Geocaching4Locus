package geocaching.api.impl.live_geocaching_api.parser;

import geocaching.api.data.User;
import geocaching.api.data.type.AttributeType;
import geocaching.api.data.type.CacheType;
import geocaching.api.data.type.ContainerType;
import geocaching.api.data.type.MemberType;
import google.gson.stream.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class JsonParser {
	private static final String TAG = "Geocaching4Locus|ParserUtil";
	
	public static Date parseJsonDate(String date) {
		Pattern DATE_PATTERN = Pattern.compile("/Date\\((\\d+)([-+]\\d{4})?\\)/");

		Matcher m = DATE_PATTERN.matcher(date);
		if (m.matches()) {
			long time = Long.parseLong(m.group(1));
			long zone = 0;
			if (m.group(2) != null && m.group(2).length() > 0)
				zone = Integer.parseInt(m.group(2)) / 100 * 1000 * 60 * 60;
			return new Date(time + zone);
		}

		Log.e(TAG, "parseJsonDate failed: " + date);
		return new Date(0);
	}
	
	protected static CacheType parseCacheType(JsonReader r) throws IOException {
		CacheType cacheType = CacheType.UnknownCache;
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("GeocacheTypeId".equals(name)) {
				cacheType = CacheType.parseCacheTypeByGroundSpeakId(r.nextInt());
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		return cacheType;
	}
	
	protected static ContainerType parseContainerType(JsonReader r) throws IOException {
		ContainerType containerType = ContainerType.NotChosen;
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("ContainerTypeId".equals(name)) {
				containerType = ContainerType.parseContainerTypeByGroundSpeakId(r.nextInt());
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		return containerType;
	}
	
	protected static MemberType parseMemberType(JsonReader r) throws IOException {
		MemberType memberType = MemberType.Basic;
		if (r.peek() == JsonToken.NULL) {
			r.nextNull();
			return memberType;
		}
		
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("MemberTypeId".equals(name)) {
				memberType = MemberType.parseMemeberTypeByGroundSpeakId(r.nextInt());
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		return memberType;
	}
	
	protected static float[] parseHomeCoordinates(JsonReader r) throws IOException {
		float[] coordinates = new float[2];
		if (r.peek() == JsonToken.NULL) {
			r.nextNull();
			return new float[] { Float.NaN, Float.NaN};
		}
		
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("Latitude".equals(name)) {
				coordinates[0] = (float) r.nextDouble();
			} else if ("Longitude".equals(name)) {
				coordinates[1] = (float) r.nextDouble();
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		return coordinates;
	}
	
	protected static User parseUser(JsonReader r) throws IOException {
		String avatarUrl = "";
		int findCount = 0;
		int hideCount = 0;
		float[] homeCoordinates = new float[] { Float.NaN, Float.NaN};
		long id = 0;
		boolean admin = false;
		MemberType memberType = null;
		String publicGuid = "";
		String userName = "";
		
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("AvatarURL".equals(name)) {
				avatarUrl = r.nextString();
			} else if ("FindCount".equals(name)) {
				findCount = r.nextInt();
			} else if ("HideCount".equals(name)) {
				hideCount = r.nextInt();
			} else if ("HomeCoordinates".equals(name)) {
				homeCoordinates = parseHomeCoordinates(r);
			} else if ("Id".equals(name)) {
				id = r.nextLong();
			} else if ("IsAdmin".equals(name)) {
				admin = r.nextBoolean();
			} else if ("MemberType".equals(name)) {
				memberType = parseMemberType(r);
			} else if ("PublicGuid".equals(name)) {
				publicGuid = r.nextString();
			} else if ("UserName".equals(name)) {
				userName = r.nextString();
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		
		return new User(avatarUrl, findCount, hideCount, homeCoordinates, id, admin, memberType, publicGuid, userName);
	}
	
	protected static AttributeType parseAttributte(JsonReader r) throws IOException {
		int id = 1;
		boolean on = false;
		
		r.beginObject();
		while(r.hasNext()) {
			String name = r.nextName();
			if ("AttributeTypeID".equals(name)) {
				id = r.nextInt();
			} else if ("IsOn".equals(name)) {
				on = r.nextBoolean();
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		
		return AttributeType.parseAttributeTypeByGroundSpeakId(id, on);
	}
	
	protected static List<AttributeType> parseAttributteList(JsonReader r) throws IOException {
		if (r.peek() != JsonToken.BEGIN_ARRAY) {
			r.skipValue();
		}
		
		List<AttributeType> list = new ArrayList<AttributeType>();
		r.beginArray();
		while(r.hasNext()) {
			list.add(parseAttributte(r));
		}
		r.endArray();
		return list;
	}	
}