package com.arcao.geocaching4locus

import android.app.Application
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.multidex.MultiDex
import com.arcao.feedback.feedbackModule
import com.arcao.geocaching4locus.authentication.util.isPremium
import com.arcao.geocaching4locus.base.constants.CrashlyticsConstants
import com.arcao.geocaching4locus.base.constants.PrefConstants
import com.arcao.geocaching4locus.base.util.AnalyticsUtil
import com.arcao.geocaching4locus.base.util.CrashlyticsTree
import com.arcao.geocaching4locus.data.account.AccountManager
import com.arcao.geocaching4locus.data.geocachingApiModule
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import io.fabric.sdk.android.Fabric
import locus.api.android.utils.LocusUtils
import locus.api.locusMapApiModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import timber.log.Timber
import java.util.UUID

class App : Application() {
    val accountManager by inject<AccountManager>()

    val deviceId: String by lazy {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)

        var value = pref.getString(PrefConstants.DEVICE_ID, null)

        if (value.isNullOrEmpty()) {
            value = UUID.randomUUID().toString()

            pref.edit {
                putString(PrefConstants.DEVICE_ID, value)
            }
        }
        value
    }

    val version: String by lazy {
        try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            Timber.e(e)
            "1.0"
        }
    }

    val versionCode: Long by lazy {
        try {
            PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        } catch (e: Exception) {
            Timber.e(e)
            0L
        }
    }

    val name: String by lazy {
        getString(applicationInfo.labelRes)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)

            if (BuildConfig.DEBUG) {
                logger(object : Logger() {
                    override fun log(level: Level, msg: MESSAGE) {
                        when (level) {
                            Level.DEBUG -> Timber.d(msg)
                            Level.INFO -> Timber.i(msg)
                            Level.ERROR -> Timber.e(msg)
                        }
                    }
                })
            }

            modules(appModule, geocachingApiModule, locusMapApiModule, feedbackModule)
        }
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().build())
//            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().build())
//        }

        prepareCrashlytics()

        AnalyticsUtil.setPremiumUser(this, accountManager.isPremium)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    private fun prepareCrashlytics() {
        // Set up Crashlytics, disabled for debug builds
        val crashlyticsKit = Crashlytics.Builder()
            .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
            .build()

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit)
        Timber.plant(CrashlyticsTree())

        Crashlytics.setUserIdentifier(deviceId)
        Crashlytics.setBool(CrashlyticsConstants.PREMIUM_MEMBER, accountManager.isPremium)

        val lv = try {
            LocusUtils.getActiveVersion(this)
        } catch (t: Throwable) {
            Timber.e(t)
            null
        }

        Crashlytics.setString(CrashlyticsConstants.LOCUS_VERSION, lv?.versionName ?: "")
        Crashlytics.setString(CrashlyticsConstants.LOCUS_PACKAGE, lv?.packageName ?: "")
    }

    @WorkerThread
    fun clearGeocachingCookies() {
        // required to call
        flushCookie()

        // setCookie acts differently when trying to expire cookies between builds of Android that are using
        // Chromium HTTP stack and those that are not. Using both of these domains to ensure it works on both.
        clearCookiesForDomain("geocaching.com")
        clearCookiesForDomain(".geocaching.com")
        clearCookiesForDomain("https://geocaching.com")
        clearCookiesForDomain("https://.geocaching.com")

        flushCookie()
    }

    private fun flushCookie() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush()
        } else {
            CookieSyncManager.createInstance(this).sync()
        }
    }

    companion object {
        private fun clearCookiesForDomain(domain: String) {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(domain) ?: return

            for (cookie in cookies.split(";").dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val cookieParts = cookie.split("=").dropLastWhile { it.isEmpty() }.toTypedArray()
                if (cookieParts.isNotEmpty()) {
                    val newCookie =
                        cookieParts[0].trim(Character::isWhitespace) + "=;expires=Sat, 1 Jan 2000 00:00:01 UTC;"
                    cookieManager.setCookie(domain, newCookie)
                }
            }
        }
    }
}
