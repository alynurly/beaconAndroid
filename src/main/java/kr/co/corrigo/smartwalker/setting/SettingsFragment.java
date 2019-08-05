/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package kr.co.corrigo.smartwalker.setting;

import no.nordicsemi.android.dfu.DfuSettingsConstants;
import kr.co.corrigo.smartwalker.R;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragment implements DfuSettingsConstants, SharedPreferences.OnSharedPreferenceChangeListener {
	private static final int SETTINGS_DEFAULT_WEIGHT = 0;
	//public static final String SETTINGS_KEEP_BOND = "settings_keep_bond";

	public CharSequence[] beaconsList={"test1", "test2"};

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		beaconsList = getArguments().getCharSequenceArray("beaconsList");
		addPreferencesFromResource(R.xml.settings);

		final SharedPreferences sp = getPreferenceManager().getSharedPreferences();
		final PreferenceScreen screen = getPreferenceScreen();
		final String lValue = sp.getString("leftBeacon", "미선택");
		final String rValue = sp.getString("rightBeacon", "미선택");
		screen.findPreference("leftBeacon").setSummary(lValue);
		screen.findPreference("rightBeacon").setSummary(rValue);

		final ListPreference listPreferenceLeft = (ListPreference)screen.findPreference("leftBeacon");
		setListPreferenceData(listPreferenceLeft);
		final ListPreference listPreferenceRight = (ListPreference)screen.findPreference("rightBeacon");
		setListPreferenceData(listPreferenceRight);

		listPreferenceLeft.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				setListPreferenceData(listPreferenceLeft);
				return false;
			}
		});

		listPreferenceRight.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				setListPreferenceData(listPreferenceRight);
				return false;
			}
		});

		// set initial values
	/*	updateNumberOfPacketsSummary();
		updateMBRSize();*/
		updatePersonalInfo();
	}

	 public void setListPreferenceData(ListPreference lp) {
		//CharSequence[] entries = beaconsList;
		//CharSequence[] entryValues = beaconsList;
		lp.setEntries(beaconsList);
		lp.setDefaultValue("미지정");
		lp.setEntryValues(beaconsList);
	}

	@Override
	public void onResume() {
		super.onResume();

		// attach the preference change listener. It will update the summary below interval preference
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		// unregister listener
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
		/*final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

		if (SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED.equals(key)) {
			final boolean disabled = !preferences.getBoolean(SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED, true);
			if (disabled) {
				new AlertDialog.Builder(getActivity()).setMessage(R.string.dfu_settings_dfu_number_of_packets_info).setTitle(R.string.dfu_settings_dfu_information)
						.setNeutralButton(R.string.ok, null).show();
			}
		} else if (SETTINGS_NUMBER_OF_PACKETS.equals(key)) {
			updateNumberOfPacketsSummary();
		} else if (SETTINGS_MBR_SIZE.equals(key)) {
			updateMBRSize();
		} else*/ if (key.equals("leftBeacon")) {
			final SharedPreferences sp = getPreferenceManager().getSharedPreferences();
			final PreferenceScreen screen = getPreferenceScreen();
			final String value = sp.getString("leftBeacon", "미선택");
			screen.findPreference("leftBeacon").setSummary(value);
		} else if (key.equals("rightBeacon")) {
			final SharedPreferences sp = getPreferenceManager().getSharedPreferences();
			final PreferenceScreen screen = getPreferenceScreen();
			final String value = sp.getString("rightBeacon", "미선택");
			screen.findPreference("rightBeacon").setSummary(value);
		} else if (key.equals("settings_body_weight")) {
			final SharedPreferences sp = getPreferenceManager().getSharedPreferences();
			final PreferenceScreen screen = getPreferenceScreen();
			final String value = sp.getString("settings_body_weight", String.valueOf(SETTINGS_DEFAULT_WEIGHT));
			screen.findPreference("settings_body_weight").setSummary(value);
		}
	}
/*
	private void updateNumberOfPacketsSummary() {
		final PreferenceScreen screen = getPreferenceScreen();
		final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

		final String value = preferences.getString(SETTINGS_NUMBER_OF_PACKETS, String.valueOf(SETTINGS_NUMBER_OF_PACKETS_DEFAULT));
		screen.findPreference(SETTINGS_NUMBER_OF_PACKETS).setSummary(value);

		final int valueInt = Integer.parseInt(value);
		if (valueInt > 200) {
			new AlertDialog.Builder(getActivity()).setMessage(R.string.dfu_settings_dfu_number_of_packets_info).setTitle(R.string.dfu_settings_dfu_information)
					.setNeutralButton(R.string.ok, null)
					.show();
		}
	}

	private void updateMBRSize() {
		final PreferenceScreen screen = getPreferenceScreen();
		final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

		final String value = preferences.getString(SETTINGS_MBR_SIZE, String.valueOf(SETTINGS_DEFAULT_MBR_SIZE));
		screen.findPreference(SETTINGS_MBR_SIZE).setSummary(value);
	}
*/

	private void updatePersonalInfo() {
		final PreferenceScreen screen = getPreferenceScreen();
		final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

		final String value = preferences.getString("settings_body_weight", String.valueOf(SETTINGS_DEFAULT_WEIGHT));
		screen.findPreference("settings_body_weight").setSummary(value);
	}
}
