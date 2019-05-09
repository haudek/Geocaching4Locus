package com.arcao.geocaching4locus.import_bookmarks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.transaction
import com.arcao.geocaching4locus.R
import com.arcao.geocaching4locus.authentication.util.AccountManager
import com.arcao.geocaching4locus.base.util.exhaustive
import com.arcao.geocaching4locus.base.util.observe
import com.arcao.geocaching4locus.base.util.showLocusMissingError
import com.arcao.geocaching4locus.import_bookmarks.fragment.BookmarkFragment
import com.arcao.geocaching4locus.import_bookmarks.fragment.BookmarkListFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ImportBookmarkActivity : AppCompatActivity() {
    private val viewModel by viewModel<ImportBookmarkViewModel>()
    private val accountManager by inject<AccountManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import_bookmark)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        @Suppress("IMPLICIT_CAST_TO_ANY")
        viewModel.action.observe(this) { action ->
            when (action) {
                ImportBookmarkAction.LocusMapNotInstalled -> {
                    showLocusMissingError()
                }
                ImportBookmarkAction.ShowList -> {
                    supportFragmentManager.transaction {
                        replace(R.id.fragment, BookmarkListFragment.newInstance())
                    }
                }
                is ImportBookmarkAction.ChooseBookmark -> {
                    supportFragmentManager.transaction {
                        addToBackStack(null)
                        replace(R.id.fragment, BookmarkFragment.newInstance(action.bookmarkList))
                    }
                }
                ImportBookmarkAction.PremiumMembershipRequired -> {
                    finish()
                }
                is ImportBookmarkAction.SignIn -> {
                    accountManager.requestSignOn(this, REQUEST_SIGN_ON)
                }
            }.exhaustive
        }

        if (savedInstanceState == null) {
            viewModel.init()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // restart update process after log in
        if (requestCode == REQUEST_SIGN_ON) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.init()
            } else {
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_SIGN_ON = 1
    }
}
