package com.arcao.geocaching4locus.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import menion.android.locus.addon.publiclib.DisplayDataExtended;
import menion.android.locus.addon.publiclib.LocusDataMapper;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.arcao.geocaching.api.GeocachingApi;
import com.arcao.geocaching.api.data.SimpleGeocache;
import com.arcao.geocaching.api.data.type.CacheType;
import com.arcao.geocaching.api.exception.GeocachingApiException;
import com.arcao.geocaching.api.exception.InvalidCredentialsException;
import com.arcao.geocaching.api.exception.InvalidSessionException;
import com.arcao.geocaching.api.impl.LiveGeocachingApi;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.DifficultyFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.Filter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.GeocacheExclusionsFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.GeocacheTypeFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.NotFoundByUsersFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.NotHiddenByUsersFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.PointRadiusFilter;
import com.arcao.geocaching.api.impl.live_geocaching_api.filter.TerrainFilter;
import com.arcao.geocaching4locus.AppConstants;
import com.arcao.geocaching4locus.MainActivity;
import com.arcao.geocaching4locus.R;
import com.arcao.geocaching4locus.UpdateActivity;
import com.arcao.geocaching4locus.provider.DataStorageProvider;
import com.arcao.geocaching4locus.util.Account;

public class SearchGeocacheService extends AbstractService {
	private static final String TAG = "SearchGeocacheService";

	public static final String PARAM_LATITUDE = "LATITUDE";
	public static final String PARAM_LONGITUDE = "LONGITUDE";

	private final static int MAX_PER_PAGE = 10;

	private static SearchGeocacheService instance = null;

	private int current;
	private int count;

	private Account account;
	private boolean showFound;
	private boolean showOwn;
	private boolean showDisabled;
	private float difficultyMin;
	private float difficultyMax;
	private float terrainMin;
	private float terrainMax;
	private boolean simpleCacheData;
	private double distance;
	private boolean importCaches;
	private int logCount;
	private int trackableCount;
	private CacheType[] cacheTypes;

	public SearchGeocacheService() {
		super(TAG, R.string.downloading, R.string.downloading);
	}

	public static SearchGeocacheService getInstance() {
		return instance;
	}

	@Override
	protected void setInstance() {
		instance = this;
	}

	@Override
	protected void removeInstance() {
		instance = null;
	}

	@Override
	protected Intent createOngoingEventIntent() {
		return new Intent(this, MainActivity.class);
	}

	public void sendProgressUpdate() {
		sendProgressUpdate(current, count);
	}

	@Override
	protected void run(Intent intent) throws Exception {
		double latitude = intent.getDoubleExtra(PARAM_LATITUDE, 0D);
		double longitude = intent.getDoubleExtra(PARAM_LONGITUDE, 0D);

		sendProgressUpdate();
		List<SimpleGeocache> caches = downloadCaches(latitude, longitude);
		sendProgressComplete(count);
		if (caches != null && caches.size() > 0)
			callLocus(caches);
	}

	@Override
	protected void loadConfiguration(SharedPreferences prefs) {
		showFound = prefs.getBoolean("filter_show_found", false);
		showOwn = prefs.getBoolean("filter_show_own", false);
		showDisabled = prefs.getBoolean("filter_show_disabled", false);
		simpleCacheData = prefs.getBoolean("simple_cache_data", false);

		difficultyMin = Float.parseFloat(prefs.getString("difficulty_filter_min", "1"));
		difficultyMax = Float.parseFloat(prefs.getString("difficulty_filter_max", "5"));
		
		terrainMin = Float.parseFloat(prefs.getString("terrain_filter_min", "1"));
		terrainMax = Float.parseFloat(prefs.getString("terrain_filter_max", "5"));
		
		String distanceString;
		if (prefs.getBoolean("imperial_units", false)) {
			distanceString = prefs.getString("filter_distance", "100");
		} else {
			distanceString = prefs.getString("filter_distance", "160.9344");
		}
		
		try {
			distance = Float.parseFloat(distanceString);
		} catch (NumberFormatException e) {
			Log.e(TAG, e.getMessage(), e);
			distance = 100;
		}
		
		if (prefs.getBoolean("imperial_units", false)) {
			// get kilometers from miles
			distance = distance * 1.609344F;
		}

		current = 0;
		count = prefs.getInt("filter_count_of_caches", 20);

		logCount = prefs.getInt("downloading_count_of_logs", 5);
		trackableCount = prefs.getInt("downloading_count_of_trackabless", 10);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String userName = prefs.getString("username", "");
		String password = prefs.getString("password", "");
		String session = prefs.getString("session", null);

		importCaches = prefs.getBoolean("import_caches", false);
		cacheTypes = getCacheTypeFilterResult(prefs);

		account = new Account(userName, password, session);
	}

	private void callLocus(List<SimpleGeocache> caches) {
		try {
			ArrayList<PointsData> pointDataCollection = new ArrayList<PointsData>();

			// beware there is row limit in DataStorageProvider (1MB per row -
			// serialized PointsData is one row)
			// so we split points into several PointsData object with same uniqueName
			// - Locus merge it automatically
			// since version 1.9.5
			PointsData points = new PointsData("Geocaching");
			for (SimpleGeocache cache : caches) {
				if (points.getPoints().size() >= 50) {
					pointDataCollection.add(points);
					points = new PointsData("Geocaching");
				}
				// convert SimpleGeocache to Point
				Point p = LocusDataMapper.toLocusPoint(this, cache);
				
				if (simpleCacheData) {
					p.setExtraOnDisplay("com.arcao.geocaching4locus", UpdateActivity.class.getName(), "simpleCacheId", cache.getCacheCode());
				}

				points.addPoint(p);
			}

			if (!points.getPoints().isEmpty())
				pointDataCollection.add(points);

			Intent intent = null;
			
			// send data via file if is possible
			File file = DisplayDataExtended.getCacheFileName(this);
			if (file != null) {
				intent = DisplayDataExtended.prepareDataFile(pointDataCollection, file);
			} else {
				intent = DisplayDataExtended.prepareDataCursor(pointDataCollection, DataStorageProvider.URI);
			}

			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			DisplayDataExtended.sendData(getApplication(), intent, importCaches);
		} catch (Exception e) {
			Log.e(TAG, "callLocus()", e);
		}
	}

	protected CacheType[] getCacheTypeFilterResult(SharedPreferences prefs) {
		Vector<CacheType> filter = new Vector<CacheType>();

		for (int i = 0; i < CacheType.values().length; i++) {
			if (prefs.getBoolean("filter_" + i, true)) {
				filter.add(CacheType.values()[i]);
			}
		}

		return filter.toArray(new CacheType[0]);
	}

	protected List<SimpleGeocache> downloadCaches(double latitude, double longitude) throws GeocachingApiException {
		final List<SimpleGeocache> caches = new ArrayList<SimpleGeocache>();

		if (account.getUserName() == null || account.getUserName().length() == 0 || account.getPassword() == null || account.getPassword().length() == 0)
			throw new InvalidCredentialsException("Username or password is empty.");

		if (isCanceled())
			return null;

		GeocachingApi api = new LiveGeocachingApi(AppConstants.CONSUMER_KEY, AppConstants.LICENCE_KEY);
		login(api, account);

		sendProgressUpdate();
		try {
			current = 0;
			int perPage = (count - current < MAX_PER_PAGE) ? count - current : MAX_PER_PAGE;
			
			while (current < count) {
				perPage = (count - current < MAX_PER_PAGE) ? count - current : MAX_PER_PAGE;

				List<SimpleGeocache> cachesToAdd;
				
				if (current == 0) {
					cachesToAdd = api.searchForGeocaches(simpleCacheData, perPage, logCount, trackableCount, new Filter[] {
							new PointRadiusFilter(latitude, longitude, (long) (distance * 1000)),
							new GeocacheTypeFilter(cacheTypes),
							new GeocacheExclusionsFilter(false, showDisabled ? null : true, null),
							new NotFoundByUsersFilter(showFound ? null : account.getUserName()),
							new NotHiddenByUsersFilter(showOwn ? null : account.getUserName()),
							new DifficultyFilter(difficultyMin, difficultyMax),
							new TerrainFilter(terrainMin, terrainMax)
					});
				} else {
					cachesToAdd = api.getMoreGeocaches(simpleCacheData, current, perPage, logCount, trackableCount);
				}

				if (isCanceled())
					return null;

				if (cachesToAdd.size() == 0)
					break;
				
				// FIX for not working distance filter
				if (computeDistance(latitude, longitude, cachesToAdd.get(cachesToAdd.size() - 1)) > distance) {
					removeCachesOverDistance(cachesToAdd, latitude, longitude, distance);
					
					if (cachesToAdd.size() == 0)
						break;
				}
				
				caches.addAll(cachesToAdd);

				current = current + perPage;

				sendProgressUpdate();
			}
			int count = caches.size();

			Log.i(TAG, "found caches: " + count);

			return caches;
		} catch (InvalidSessionException e) {
			account.setSession(null);
			removeSession();

			return downloadCaches(latitude, longitude);
		} finally {
			account.setSession(api.getSession());
			if (account.getSession() != null && account.getSession().length() > 0) {
				storeSession(account.getSession());
			}
		}
	}
	
	protected void removeCachesOverDistance(List<SimpleGeocache> caches, double latitude, double longitude, double maxDistance) {
		while (caches.size() > 0) {
			SimpleGeocache cache = caches.get(caches.size() - 1);
			double distance = computeDistance(latitude, longitude, cache); 
			
			if (distance > maxDistance) {
				Log.i(TAG, "Cache " + cache.getCacheCode() + " is over distance.");
				caches.remove(cache);
			} else {
				return;
			}
		}
	}

	protected double computeDistance(double latitude, double longitude, SimpleGeocache cache) {
    final double r = 6366.707;

    // convert to radians
    double latitudeFrom = Math.toRadians(latitude);
    double longitudeFrom = Math.toRadians(longitude);
    double latitudeTo = Math.toRadians(cache.getLatitude());
    double longitudeTo = Math.toRadians(cache.getLongitude());

    return Math.acos(Math.sin(latitudeFrom) * Math.sin(latitudeTo) + Math.cos(latitudeFrom) * Math.cos(latitudeTo) * Math.cos(longitudeTo - longitudeFrom)) * r;
  } 

	private void login(GeocachingApi api, Account account) throws GeocachingApiException, InvalidCredentialsException {
		try {
			if (account.getSession() == null || account.getSession().length() == 0) {
				api.openSession(account.getUserName(), account.getPassword());
			} else {
				api.openSession(account.getSession());
			}
		} catch (InvalidCredentialsException e) {
			Log.e(TAG, "Creditials not valid.", e);
			throw e;
		}
	}
}
