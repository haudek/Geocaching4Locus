package com.arcao.feedback.collector

import android.content.Context
import com.arcao.geocaching4locus.App
import com.arcao.geocaching4locus.base.constants.PrefConstants

class AccountInfoCollector(private val context: Context) : Collector() {
    private val app = context.applicationContext as App

    override val name: String
        get() = "AccountInfo"

    override fun collect(): String {
        val sb = StringBuilder()

        val account = app.accountManager.account
        if (account == null) {
            sb.append("No Account").append("\n")
        } else {
            sb.append("NAME=").append(account.name()).append("\n")

            sb.append("\n--- RESTRICTIONS ---\n")
            sb.append(SharedPreferencesCollector(context, PrefConstants.RESTRICTION_STORAGE_NAME).collect())
        }
        return sb.toString()
    }
}
