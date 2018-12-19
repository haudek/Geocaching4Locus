package com.arcao.geocaching4locus.settings.manager

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.arcao.geocaching4locus.base.constants.PrefConstants

class DefaultPreferenceManager(
    context: Context
) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val showLiveMapDisabledNotification
        get() = preferences.getBoolean(PrefConstants.SHOW_LIVE_MAP_DISABLED_NOTIFICATION, false)

    val hideGeocachesOnLiveMapDisabled
        get() = preferences.getBoolean(PrefConstants.LIVE_MAP_HIDE_CACHES_ON_DISABLED, false)

    val disableDnfNmNaGeocaches
        get() = preferences.getBoolean(PrefConstants.DOWNLOADING_DISABLE_DNF_NM_NA_CACHES, false)

    val disableDnfNmNaGeocachesThreshold
        get() = preferences.getInt(PrefConstants.DOWNLOADING_DISABLE_DNF_NM_NA_CACHES_LOGS_COUNT, 1)

    var liveMapLastRequests
        get() = preferences.getInt(PrefConstants.LIVE_MAP_LAST_REQUESTS, 0)
        set(value) = preferences.edit { putInt(PrefConstants.LIVE_MAP_LAST_REQUESTS, value) }
}
