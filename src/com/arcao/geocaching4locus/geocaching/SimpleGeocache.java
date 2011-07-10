package com.arcao.geocaching4locus.geocaching;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import android.location.Location;

public class SimpleGeocache {
	private final String geoCode;
	private final String name;
	private final double longitude;
	private final double latitude;
	private final CacheType cacheType;
	private final float difficultyRating;
	private final float terrainRating;
	private final String authorGuid;
	private final String authorName;
	private final boolean available;
	private final boolean archived;
	private final boolean premiumListing;
	private final String countryName;
	private final String stateName;
	private final Date created;
	private final String contactName;
	private final ContainerType containerType;
	private final int trackableCount;
	private final boolean found;
	
	
	
	public SimpleGeocache(String geoCode, String name, double longitude,
			double latitude, CacheType cacheType, float difficultyRating,
			float terrainRating, String authorGuid, String authorName,
			boolean available, boolean archived, boolean premiumListing,
			String countryName, String stateName, Date created,
			String contactName, ContainerType containerType,
			int trackableCount, boolean found) {
		this.geoCode = geoCode;
		this.name = name;
		this.longitude = longitude;
		this.latitude = latitude;
		this.cacheType = cacheType;
		this.difficultyRating = difficultyRating;
		this.terrainRating = terrainRating;
		this.authorGuid = authorGuid;
		this.authorName = authorName;
		this.available = available;
		this.archived = archived;
		this.premiumListing = premiumListing;
		this.countryName = countryName;
		this.stateName = stateName;
		this.created = created;
		this.contactName = contactName;
		this.containerType = containerType;
		this.trackableCount = trackableCount;
		this.found = found;
	}

	public String getGeoCode() {
		return geoCode;
	}

	public String getName() {
		return name;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public CacheType getCacheType() {
		return cacheType;
	}

	public float getDifficultyRating() {
		return difficultyRating;
	}

	public float getTerrainRating() {
		return terrainRating;
	}

	public String getAuthorGuid() {
		return authorGuid;
	}

	public String getAuthorName() {
		return authorName;
	}

	public boolean isAvailable() {
		return available;
	}

	public boolean isArchived() {
		return archived;
	}

	public boolean isPremiumListing() {
		return premiumListing;
	}

	public String getCountryName() {
		return countryName;
	}

	public String getStateName() {
		return stateName;
	}

	public Date getCreated() {
		return created;
	}

	public String getContactName() {
		return contactName;
	}

	public ContainerType getContainerType() {
		return containerType;
	}

	public int getTrackableCount() {
		return trackableCount;
	}

	public boolean isFound() {
		return found;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Method m : getClass().getMethods()) {
			if ((!m.getName().startsWith("get") && !m.getName().startsWith("is")) ||
			    m.getParameterTypes().length != 0 ||  
			    void.class.equals(m.getReturnType()))
			    continue;
			
			sb.append(m.getName());
			sb.append(':');
			try {
				sb.append(m.invoke(this, new Object[0]));
			} catch (Exception e) {}
			sb.append(", ");
		}
		return sb.toString();
	}
	
	public static SimpleGeocache load(DataInputStream dis) throws IOException {
		return new SimpleGeocache(
				dis.readUTF(),
				dis.readUTF(),
				dis.readDouble(),
				dis.readDouble(),
				CacheType.parseCacheType(dis.readUTF()),
				dis.readFloat(),
				dis.readFloat(),
				dis.readUTF(),
				dis.readUTF(),
				dis.readBoolean(),
				dis.readBoolean(),
				dis.readBoolean(),
				dis.readUTF(),
				dis.readUTF(),
				new Date(dis.readLong()),
				dis.readUTF(),
				ContainerType.parseContainerType(dis.readUTF()),
				dis.readInt(),
				dis.readBoolean()
		);
	}
	
	public void store(DataOutputStream dos) throws IOException {
		dos.writeUTF(getGeoCode());
		dos.writeUTF(getName());
		dos.writeDouble(getLongitude());
		dos.writeDouble(getLatitude());
		dos.writeUTF(getCacheType().toString());
		dos.writeFloat(getDifficultyRating());
		dos.writeFloat(getTerrainRating());
		dos.writeUTF(getAuthorGuid());
		dos.writeUTF(getAuthorName());
		dos.writeBoolean(isAvailable());
		dos.writeBoolean(isArchived());
		dos.writeBoolean(isPremiumListing());
		dos.writeUTF(getCountryName());
		dos.writeUTF(getStateName());
		dos.writeLong(getCreated().getTime());
		dos.writeUTF(getContactName());
		dos.writeUTF(getContainerType().toString());
		dos.writeInt(getTrackableCount());
		dos.writeBoolean(isFound());
	}
	
	public Point toPoint() {
		Location loc = new Location(getClass().getName());
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		
		Point p = new Point(name, loc);
		
		PointGeocachingData d = new PointGeocachingData();
		d.cacheID = geoCode;
		d.name = name;
		d.type = convertCacheTypeToPointCacheType(cacheType);
		d.difficulty = difficultyRating;
		d.terrain = terrainRating;
		d.owner = authorName;
		d.available = available;
		d.archived = archived;
		d.premiumOnly = premiumListing;
		d.country = countryName;
		d.state = stateName;
		d.hidden = created.toLocaleString();
		d.container = convertContainerTypeToPointContainerType(containerType);
		
		p.setGeocachingData(d);
		return p;
	}

	private int convertContainerTypeToPointContainerType(ContainerType containerType) {
		switch (containerType) {
		case Micro:
			return PointGeocachingData.CACHE_SIZE_MICRO;
		case Small:
			return PointGeocachingData.CACHE_SIZE_SMALL;
		case Regular:
			return PointGeocachingData.CACHE_SIZE_REGULAR;
		case Large:
			return PointGeocachingData.CACHE_SIZE_LARGE;
		case NotChosen:
			return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
		case Other:
		case Virtual:
		default:
			return PointGeocachingData.CACHE_SIZE_OTHER;
		}
	}

	private int convertCacheTypeToPointCacheType(CacheType cacheType) {
		switch (cacheType) {
		case EarthCache:
			return PointGeocachingData.CACHE_TYPE_EARTH;
		case EventCache:
			return PointGeocachingData.CACHE_TYPE_EVENT;
		case GpsAdventuresExhibit:
			return PointGeocachingData.CACHE_TYPE_GPS_ADVENTURE;
		case LetterboxHybrid:
			return PointGeocachingData.CACHE_TYPE_LETTERBOX;
		case LocationlessCache:
			return PointGeocachingData.CACHE_TYPE_LOCATIONLESS;
		case MultiCache:
			return PointGeocachingData.CACHE_TYPE_MULTI;
		case ProjectApeCache:
			return PointGeocachingData.CACHE_TYPE_PROJECT_APE;
		case TraditionalCache:
			return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
		case UnknownCache:
			return PointGeocachingData.CACHE_TYPE_MYSTERY;
		case VirtualCache:
			return PointGeocachingData.CACHE_TYPE_VIRTUAL;
		case WebcamCache:
			return PointGeocachingData.CACHE_TYPE_WEBCAM;
		case WherigoCache:
			return PointGeocachingData.CACHE_TYPE_WHERIGO;
		default:
			return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
		}
	}
	
	
}
