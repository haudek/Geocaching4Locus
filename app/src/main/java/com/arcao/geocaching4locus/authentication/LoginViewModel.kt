package com.arcao.geocaching4locus.authentication

import android.os.Build
import androidx.annotation.UiThread
import com.arcao.geocaching4locus.App
import com.arcao.geocaching4locus.authentication.usecase.CreateAccountUseCase
import com.arcao.geocaching4locus.authentication.usecase.RetrieveAuthorizationUrlUseCase
import com.arcao.geocaching4locus.base.BaseViewModel
import com.arcao.geocaching4locus.base.ProgressState
import com.arcao.geocaching4locus.base.constants.CrashlyticsConstants
import com.arcao.geocaching4locus.base.coroutine.CoroutinesDispatcherProvider
import com.arcao.geocaching4locus.base.util.AnalyticsUtil
import com.arcao.geocaching4locus.base.util.Command
import com.arcao.geocaching4locus.base.util.invoke
import com.arcao.geocaching4locus.data.account.AccountManager
import com.arcao.geocaching4locus.error.handler.ExceptionHandler
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.cancelChildren

class LoginViewModel(
    private val app: App,
    private val retrieveAuthorizationUrl: RetrieveAuthorizationUrlUseCase,
    private val createAccount: CreateAccountUseCase,
    private val exceptionHandler: ExceptionHandler,
    private val accountManager: AccountManager,
    dispatcherProvider: CoroutinesDispatcherProvider
) : BaseViewModel(dispatcherProvider) {

    val action = Command<LoginAction>()

    fun startLogin() {
        if (job.isActive) coroutineContext.cancelChildren()

        mainLaunch {
            try {
                showProgress {
                    app.clearGeocachingCookies()

                    // retrieve authorization url
                    val url = retrieveAuthorizationUrl()

                    action(LoginAction.LoginUrlAvailable(url))
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun finishLogin(input: String) = mainLaunch {
        if (input.isBlank()) {
            action(LoginAction.Cancel)
            return@mainLaunch
        }

        try {
            showProgress {
                // create account
                val account = createAccount(input)

                val premium = account.isPremium()

                // handle analytics and crashlytics
                Crashlytics.setBool(CrashlyticsConstants.PREMIUM_MEMBER, premium)
                AnalyticsUtil.setPremiumUser(app, premium)
                AnalyticsUtil.actionLogin(true, premium)

                action(LoginAction.Finish(!premium))
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private suspend fun handleException(e: Exception) = mainContext {
        accountManager.deleteAccount()
        AnalyticsUtil.actionLogin(success = false, premiumMember = false)

        action(LoginAction.Error(exceptionHandler(e)))
    }

    fun isCompatLoginRequired() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    fun showProgress() {
        progress(ProgressState.ShowProgress())
    }

    fun hideProgress() {
        progress(ProgressState.HideProgress)
    }

    @UiThread
    fun cancelLogin() {
        AnalyticsUtil.actionLogin(success = false, premiumMember = false)

        job.cancel()
        action(LoginAction.Cancel)
    }
}