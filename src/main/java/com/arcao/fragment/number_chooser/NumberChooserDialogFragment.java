package com.arcao.fragment.number_chooser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.arcao.geocaching4locus.R;

import java.lang.ref.WeakReference;

public class NumberChooserDialogFragment extends DialogFragment {
	public static final String PARAM_TITLE = "TITLE";
	public static final String PARAM_PREFIX_TEXT = "PREFIX_TEXT";
	public static final String PARAM_DEFAULT_VALUE = "DEFAULT_VALUE";
	public static final String PARAM_MIN_VALUE = "MIN_VALUE";
	public static final String PARAM_MAX_VALUE = "MAX_VALUE";
	public static final String PARAM_STEP = "STEP";

	protected int mMinValue = 0;
	protected int mMaxValue = 100;
	protected int mStep = 1;
	protected int mValue = mMinValue;
	protected int mNewValue = mValue;

	protected int mPrefixTextRes = 0;

	protected WeakReference<OnNumberChooserDialogClosedListener> onNumberChooserDialogClosedListenerRef;

	public static NumberChooserDialogFragment newInstance(int titleRes, int prefixQuantityRes, int minValue, int maxValue, int defaultValue) {
		return newInstance(titleRes, prefixQuantityRes, minValue, maxValue, defaultValue, 1);
	}

	public static NumberChooserDialogFragment newInstance(int titleRes, int prefixQuantityRes, int minValue, int maxValue, int defaultValue, int step) {
		NumberChooserDialogFragment fragment;

		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
			fragment = new NumberChooserDialogFragmentHoneycomb();
		} else {
			fragment = new NumberChooserDialogFragment();
		}

		Bundle args = new Bundle();
		args.putInt(PARAM_TITLE, titleRes);
		args.putInt(PARAM_PREFIX_TEXT, prefixQuantityRes);
		args.putInt(PARAM_MIN_VALUE, minValue);
		args.putInt(PARAM_MAX_VALUE, maxValue);
		args.putInt(PARAM_DEFAULT_VALUE, defaultValue);
		args.putInt(PARAM_STEP, step);
		fragment.setArguments(args);
		fragment.setCancelable(false);

		return fragment;
	}

	public int getValue() {
		return mNewValue;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onNumberChooserDialogClosedListenerRef = new WeakReference<>((OnNumberChooserDialogClosedListener) activity);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnNumberChooserDialogClosedListener");
		}
	}

	protected void fireOnNumberChooserDialogClosedListener() {
		OnNumberChooserDialogClosedListener listener = onNumberChooserDialogClosedListenerRef.get();
		if (listener != null) {
			listener.onNumberChooserDialogClosed(this);
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mMinValue = getArguments().getInt(PARAM_MIN_VALUE, mMinValue);
		mMaxValue = getArguments().getInt(PARAM_MAX_VALUE, mMaxValue);
		mStep = getArguments().getInt(PARAM_STEP, mStep);
		mValue = getArguments().getInt(PARAM_DEFAULT_VALUE, mValue);

		if (savedInstanceState != null ) {
			mValue = savedInstanceState.getInt(PARAM_DEFAULT_VALUE, mValue);
		}

		if (mValue % mStep != 0) {
			mValue = (mValue / mStep) * mStep;
		}

		if (mValue < mMinValue) {
			mValue = mMinValue;
		}

		if (mValue > mMaxValue) {
			mValue = mMaxValue;
		}

		mPrefixTextRes = getArguments().getInt(PARAM_PREFIX_TEXT, mPrefixTextRes);

		mNewValue = mValue;

		return new AlertDialog.Builder(getActivity())
			.setTitle(getArguments().getInt(PARAM_TITLE))
			.setView(prepareView())
			.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mNewValue = mValue;
					fireOnNumberChooserDialogClosedListener();
				}
			})
			.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fireOnNumberChooserDialogClosedListener();
				}
			})
			.create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(PARAM_DEFAULT_VALUE, mValue);
		super.onSaveInstanceState(outState);
	}

	protected View prepareView() {
		View view = getActivity().getLayoutInflater().inflate(R.layout.number_picker_dialog, null);

		final TextView textView = (TextView) view.findViewById(R.id.number_picker_dialog_prefix_text);
		textView.setText(getResources().getQuantityString(mPrefixTextRes, mValue, mValue));

		// SeekBar doesn't support minimal value, we must transpose values
		SeekBar seekBar =	(SeekBar) view.findViewById(R.id.number_picker_dialog_seek_bar);
		seekBar.setMax((mMaxValue - mMinValue) / mStep);
		seekBar.setProgress((mValue - mMinValue) / mStep);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mValue = progress * mStep + mMinValue;
				textView.setText(getResources().getQuantityString(mPrefixTextRes, mValue, mValue));
			}
		});

		return view;
	}

	// --------------------------- Helper methods ------------------------------------------

	// This is to work around what is apparently a bug. If you don't have it
	// here the dialog will be dismissed on rotation, so tell it not to dismiss.
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setDismissMessage(null);
		super.onDestroyView();
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		FragmentTransaction ft = manager.beginTransaction();
		Fragment prev = manager.findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev).commitAllowingStateLoss();
			manager.executePendingTransactions();
		}

		super.show(manager, tag);
	}

	// --------------------------- Listeners -----------------------------------------------

	public interface OnNumberChooserDialogClosedListener {
		public void onNumberChooserDialogClosed(NumberChooserDialogFragment fragment);
	}

}