package com.arcao.geocaching4locus.importgc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.arcao.geocaching4locus.authentication.util.AccountManager
import com.arcao.geocaching4locus.base.AbstractActionBarActivity
import com.arcao.geocaching4locus.base.util.exhaustive
import com.arcao.geocaching4locus.base.util.observe
import com.arcao.geocaching4locus.base.util.showLocusMissingError
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ImportUrlActivity : AbstractActionBarActivity() {
    private val viewModel by viewModel<ImportUrlViewModel>()
    private val accountManager by inject<AccountManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.action.observe(this, ::handleAction)
        viewModel.progress.observe(this, ::handleProgress)

        if (savedInstanceState == null) {
            processIntent()
        }
    }

    override fun onProgressCancel(requestId: Int) {
        viewModel.cancelImport()
    }

    private fun processIntent() {
        val uri = intent.data
        if (uri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        viewModel.startImport(uri)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun handleAction(action: ImportUrlAction) {
        Timber.v("handleAction: %s", action)

        when (action) {
            is ImportUrlAction.Cancel -> {
                setResult(Activity.RESULT_CANCELED)
                onBackPressed()
            }
            is ImportUrlAction.Error -> {
                startActivity(action.intent)
                setResult(Activity.RESULT_CANCELED)
                onBackPressed()
            }
            is ImportUrlAction.Finish -> {
                startActivity(action.intent)
                setResult(Activity.RESULT_OK)
                finish()
            }
            is ImportUrlAction.LocusMapNotInstalled -> {
                showLocusMissingError()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            is ImportUrlAction.SignIn -> {
                accountManager.requestSignOn(this, REQUEST_SIGN_ON)
            }
        }.exhaustive
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SIGN_ON) {
            if (resultCode == Activity.RESULT_OK) {
                processIntent()
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_SIGN_ON = 1
    }
}