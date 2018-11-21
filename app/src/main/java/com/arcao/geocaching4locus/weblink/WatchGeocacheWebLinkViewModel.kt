package com.arcao.geocaching4locus.weblink

import android.net.Uri
import com.arcao.geocaching.api.util.GeocachingUtils
import com.arcao.geocaching4locus.BuildConfig
import com.arcao.geocaching4locus.authentication.util.AccountManager
import com.arcao.geocaching4locus.base.coroutine.CoroutinesDispatcherProvider
import com.arcao.geocaching4locus.base.usecase.GetPointFromGeocacheCodeUseCase
import com.arcao.geocaching4locus.error.handler.ExceptionHandler
import locus.api.objects.extra.Point
import java.util.*

class WatchGeocacheWebLinkViewModel(
    accountManager: AccountManager,
    getPointFromGeocacheCodeUseCase: GetPointFromGeocacheCodeUseCase,
    exceptionHandler: ExceptionHandler,
    dispatcherProvider: CoroutinesDispatcherProvider
) : WebLinkViewModel(accountManager, getPointFromGeocacheCodeUseCase, exceptionHandler, dispatcherProvider) {

    override fun getWebLink(point: Point): Uri {
        val cacheId = GeocachingUtils.cacheCodeToCacheId(point.gcData.cacheID)

        return if (BuildConfig.GEOCACHING_API_STAGING) {
            Uri.parse(String.format(Locale.ROOT, URL_FORMAT_STAGING, cacheId))
        } else {
            Uri.parse(String.format(Locale.ROOT, URL_FORMAT, cacheId))
        }
    }

    companion object {
        private const val URL_FORMAT = "https://www.geocaching.com/my/watchlist.aspx?w=%d"
        private const val URL_FORMAT_STAGING = "https://staging.geocaching.com/my/watchlist.aspx?w=%d"
    }
}
