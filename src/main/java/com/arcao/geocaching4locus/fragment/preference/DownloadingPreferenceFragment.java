package com.arcao.geocaching4locus.fragment.preference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import com.arcao.geocaching4locus.App;
import com.arcao.geocaching4locus.R;
import com.arcao.geocaching4locus.preference.SeekBarPreference;
import com.arcao.preference.ListPreference;

public class DownloadingPreferenceFragment extends AbstractPreferenceFragment {
	@Override
	public void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preference_category_downloading);
	}

	@Override
	protected void preparePreference() {
		final CheckBoxPreference simpleCacheDataPreference = findPreference(DOWNLOADING_SIMPLE_CACHE_DATA, CheckBoxPreference.class);
		final ListPreference fullCacheDataOnShowPreference = findPreference(DOWNLOADING_FULL_CACHE_DATE_ON_SHOW, ListPreference.class);
		final SeekBarPreference downloadingCountOfLogsPreference = findPreference(DOWNLOADING_COUNT_OF_LOGS, SeekBarPreference.class);
		final ListPreference countOfCachesStepPreference = findPreference(DOWNLOADING_COUNT_OF_CACHES_STEP, ListPreference.class);

		boolean premiumMember = App.get(getActivity()).getAuthenticatorHelper().getRestrictions().isPremiumMember();

//		if (!premiumMember)
//			simpleCacheDataPreference.setChecked(true);

		//simpleCacheDataPreference.setEnabled(premiumMember);
		simpleCacheDataPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				fullCacheDataOnShowPreference.setEnabled((Boolean) newValue);
				return true;
			}
		});
		fullCacheDataOnShowPreference.setEnabled(simpleCacheDataPreference.isChecked());
		fullCacheDataOnShowPreference.setSummary(preparePreferenceSummary(fullCacheDataOnShowPreference.getEntry(), R.string.pref_download_on_show_summary));

		downloadingCountOfLogsPreference.setSummary(preparePreferenceSummary(String.valueOf(downloadingCountOfLogsPreference.getProgress()),
						R.string.pref_count_of_logs_summary));

		countOfCachesStepPreference.setSummary(preparePreferenceSummary(countOfCachesStepPreference.getEntry(), R.string.pref_downloading_count_of_caches_step_summary));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);

		if (key == null)
			return;

		switch (key) {
			case DOWNLOADING_COUNT_OF_LOGS:
				final SeekBarPreference countOfLogsPreference = findPreference(key, SeekBarPreference.class);
				countOfLogsPreference.setSummary(preparePreferenceSummary(String.valueOf(countOfLogsPreference.getProgress()), R.string.pref_count_of_logs_summary));
				break;

			case DOWNLOADING_COUNT_OF_CACHES_STEP:
				final ListPreference countOfCachesStepPreference = findPreference(key, ListPreference.class);
				countOfCachesStepPreference.setSummary(preparePreferenceSummary(countOfCachesStepPreference.getEntry(), R.string.pref_downloading_count_of_caches_step_summary));
				break;

			case DOWNLOADING_FULL_CACHE_DATE_ON_SHOW:
				final ListPreference fullCacheDataOnShowPreference = findPreference(key, ListPreference.class);
				fullCacheDataOnShowPreference.setSummary(preparePreferenceSummary(fullCacheDataOnShowPreference.getEntry(), R.string.pref_download_on_show_summary));
				break;
		}
	}
}