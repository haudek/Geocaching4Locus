package com.arcao.geocaching4locus.importgc.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.arcao.geocaching4locus.R
import com.arcao.geocaching4locus.base.fragment.AbstractDialogFragment
import com.arcao.geocaching4locus.base.util.runIfIs
import com.arcao.geocaching4locus.importgc.ImportGeocacheCodeViewModel
import com.google.android.material.textfield.TextInputLayout
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class GeocacheCodesInputDialogFragment : AbstractDialogFragment() {
    val model by sharedViewModel<ImportGeocacheCodeViewModel>()

    private lateinit var editTextView: EditText
    lateinit var textInputLayout: TextInputLayout

    interface DialogListener {
        fun onInputFinished(input: Array<String>)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (isShowing) {
            outState.putCharSequence(STATE_INPUT, editTextView.text)
            outState.putCharSequence(STATE_ERROR_MESSAGE, textInputLayout.error)
        }
    }

    private fun fireOnInputFinished(@Nullable input: Array<String>?) {
        activity.runIfIs(DialogListener::class) {
            onInputFinished(input ?: emptyArray())
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        fireOnInputFinished(null)
        super.onCancel(dialog)
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog(requireContext())
            .title(R.string.title_import_from_gc)
            .customView(R.layout.dialog_gc_number_input)
            .noAutoDismiss()
            .positiveButton(R.string.button_ok) { dialog ->
                try {
                    val geocacheCodes = model.parseGeocacheCodes(editTextView.text)
                    fireOnInputFinished(geocacheCodes)
                    dialog.dismiss()
                } catch (e: Exception) {
                    textInputLayout.error = getText(R.string.error_gc_code_invalid)
                }
            }
            .negativeButton(R.string.button_cancel) { dialog ->
                fireOnInputFinished(null)
                dialog.dismiss()
            }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        textInputLayout =
            dialog.getCustomView() as? TextInputLayout ?: throw IllegalStateException("Custom view is null")

        val positiveButton = dialog.getActionButton(WhichButton.POSITIVE)

        editTextView = textInputLayout.findViewById(R.id.input)
        editTextView.nextFocusDownId = positiveButton.id
        editTextView.setText(R.string.prefix_gc)
        editTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textInputLayout.error != null) {
                    textInputLayout.error = null
                }
            }
        })

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_INPUT)) {
            editTextView.setText(savedInstanceState.getCharSequence(STATE_INPUT))
            textInputLayout.error = savedInstanceState.getCharSequence(STATE_ERROR_MESSAGE)
        }

        // move caret on a last position
        editTextView.setSelection(editTextView.text.length)

        return dialog
    }

    companion object {
        private const val STATE_INPUT = "input"
        private const val STATE_ERROR_MESSAGE = "error_message"

        fun newInstance(): GeocacheCodesInputDialogFragment {
            return GeocacheCodesInputDialogFragment()
        }
    }
}
