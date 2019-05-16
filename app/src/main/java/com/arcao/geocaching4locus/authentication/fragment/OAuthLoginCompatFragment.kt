package com.arcao.geocaching4locus.authentication.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.arcao.geocaching4locus.R
import com.arcao.geocaching4locus.authentication.LoginActivity
import com.arcao.geocaching4locus.base.util.observe
import com.arcao.geocaching4locus.base.util.showWebPage

class OAuthLoginCompatFragment : BaseOAuthLoginFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.progress.observe(this) {
            (requireActivity() as LoginActivity).handleProgress(it)
        }
        viewModel.action.observe(this, ::handleAction)

        if (savedInstanceState == null)
            viewModel.startLogin()
    }

    override fun onLoginUrlAvailable(url: String) {
        MaterialDialog(requireActivity())
            .message(R.string.warning_compat_android_sign_in)
            .positiveButton(R.string.button_ok) {
                showOAuthVerifierDialog()
                requireActivity().showWebPage(Uri.parse(url))
            }
            .negativeButton(R.string.button_cancel) {
                viewModel.cancelLogin()
            }
            .cancelable(false)
            .cancelOnTouchOutside(false)
            .show()
    }

    private fun showOAuthVerifierDialog() {
        MaterialDialog(requireActivity())
            .message(R.string.message_enter_authorization_code)
            .input(hintRes = R.string.hint_authorization_code, allowEmpty = false) { _, input ->
                viewModel.finishLogin(input.toString())
            }
            .positiveButton(R.string.button_ok)
            .negativeButton(R.string.button_cancel) {
                viewModel.cancelLogin()
            }
            .cancelable(false)
            .cancelOnTouchOutside(false)
            .show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_oauth_compat, container, false)
    }

    companion object {
        fun newInstance(): OAuthLoginCompatFragment {
            return OAuthLoginCompatFragment()
        }
    }
}