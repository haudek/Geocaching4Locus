package com.arcao.geocaching4locus.live_map

import android.content.Context
import android.content.Intent
import com.arcao.geocaching.api.data.coordinates.Coordinates
import com.arcao.geocaching.api.exception.GeocachingApiException
import com.arcao.geocaching.api.exception.InvalidCredentialsException
import com.arcao.geocaching.api.exception.NetworkException
import com.arcao.geocaching4locus.R
import com.arcao.geocaching4locus.base.BaseViewModel
import com.arcao.geocaching4locus.base.constants.AppConstants
import com.arcao.geocaching4locus.base.coroutine.CoroutinesDispatcherProvider
import com.arcao.geocaching4locus.base.usecase.GetPointsFromRectangleCoordinatesUseCase
import com.arcao.geocaching4locus.base.usecase.RemoveLocusMapPointsUseCase
import com.arcao.geocaching4locus.base.usecase.SendPointsSilentToLocusMapUseCase
import com.arcao.geocaching4locus.base.util.getText
import com.arcao.geocaching4locus.error.exception.LocusMapRuntimeException
import com.arcao.geocaching4locus.live_map.util.LiveMapNotificationManager
import com.arcao.geocaching4locus.settings.manager.DefaultPreferenceManager
import com.arcao.geocaching4locus.settings.manager.FilterPreferenceManager
import com.arcao.geocaching4locus.update.UpdateActivity
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.map
import timber.log.Timber

class LiveMapViewModel(
    private val context: Context,
    private val notificationManager: LiveMapNotificationManager,
    private val filterPreferenceManager: FilterPreferenceManager,
    private val defaultPreferenceManager: DefaultPreferenceManager,
    private val getPointsFromRectangleCoordinates: GetPointsFromRectangleCoordinatesUseCase,
    private val sendPointsSilentToLocusMap: SendPointsSilentToLocusMapUseCase,
    private val removeLocusMapPoints: RemoveLocusMapPointsUseCase,
    dispatcherProvider: CoroutinesDispatcherProvider
) : BaseViewModel(dispatcherProvider) {

    fun addTask(intent: Intent, completionCallback: (Intent) -> Unit) {
        cancelTasks()

        downloadLiveMapGeocaches(intent).invokeOnCompletion {
            completionCallback(intent)
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun downloadLiveMapGeocaches(task: Intent) = computationLaunch {
        var requests = 0

        try {
            var count = AppConstants.ITEMS_PER_REQUEST
            var receivedGeocaches = 0

            showProgress(maxProgress = count) {
                val pointListChannel = getPointsFromRectangleCoordinates(
                    task.getCoordinates(LiveMapService.PARAM_LATITUDE, LiveMapService.PARAM_LONGITUDE),
                    task.getCoordinates(LiveMapService.PARAM_TOP_LEFT_LATITUDE, LiveMapService.PARAM_TOP_LEFT_LONGITUDE),
                    task.getCoordinates(LiveMapService.PARAM_BOTTOM_RIGHT_LATITUDE, LiveMapService.PARAM_BOTTOM_RIGHT_LONGITUDE),
                    filterPreferenceManager.simpleCacheData,
                    false,
                    filterPreferenceManager.geocacheLogsCount,
                    filterPreferenceManager.trackableLogsCount,
                    filterPreferenceManager.showDisabled,
                    filterPreferenceManager.showFound,
                    filterPreferenceManager.showOwn,
                    filterPreferenceManager.geocacheTypes,
                    filterPreferenceManager.containerTypes,
                    filterPreferenceManager.difficultyMin,
                    filterPreferenceManager.difficultyMax,
                    filterPreferenceManager.terrainMin,
                    filterPreferenceManager.terrainMax,
                    filterPreferenceManager.excludeIgnoreList,
                    AppConstants.LIVEMAP_CACHES_COUNT
                ) { count = it }.map { list ->
                    receivedGeocaches += list.size
                    requests++

                    updateProgress(progress = receivedGeocaches, maxProgress = count)

                    list.forEach { point ->
                        point.setExtraOnDisplay(
                            context.packageName,
                            UpdateActivity::class.java.name,
                            UpdateActivity.PARAM_SIMPLE_CACHE_ID,
                            point.gcData.cacheID
                        )
                    }
                    list
                }

                // send to locus map
                sendPointsSilentToLocusMap(AppConstants.LIVEMAP_PACK_WAYPOINT_PREFIX, pointListChannel)
            }
        } catch (e: Exception) {
            handleException(e)
        } finally {
            val lastRequests = defaultPreferenceManager.liveMapLastRequests
            if (requests < lastRequests) {
                removeLocusMapPoints(AppConstants.LIVEMAP_PACK_WAYPOINT_PREFIX, requests + 1, lastRequests)
            }
            defaultPreferenceManager.liveMapLastRequests = requests
        }
    }

    fun cancelTasks() {
        coroutineContext.cancelChildren()
    }

    private fun handleException(e: Exception) {
        Timber.e(e)

        when (e) {
            is LocusMapRuntimeException -> {
                notificationManager.showLiveMapToast(context.getText(R.string.error_locus_map, e.message ?: ""))
                // disable live map
                notificationManager.isLiveMapEnabled = false
            }
            is InvalidCredentialsException -> {
                notificationManager.showLiveMapToast(R.string.error_no_account)
                // disable live map
                notificationManager.isLiveMapEnabled = false
            }
            is NetworkException -> {
                notificationManager.showLiveMapToast(R.string.error_network_unavailable)
            }
            is GeocachingApiException -> {
                notificationManager.showLiveMapToast(e.message ?: "")
            }
            else -> throw e
        }
    }
}

private fun Intent.getCoordinates(latitudeName: String, longitudeName: String) = Coordinates.create(
    getDoubleExtra(latitudeName, 0.0),
    getDoubleExtra(longitudeName, 0.0)
)

