/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.provider.Settings;

import java.util.Observable;
import java.util.Observer;
import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.app.AlertDialog;
import android.app.Dialog;
import android.location.LocationManager;
import android.widget.Toast;


/**
 * Gesture lock pattern settings.
 */
public class LocationSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    // Location Settings
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_GPS = "location_gps";
    private static final String KEY_ASSISTED_GPS = "assisted_gps";
    private static final String KEY_USE_LOCATION = "location_use_for_services";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;
    private CheckBoxPreference mUseLocation;
    private ListPreference mGPSBTPref;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();

        mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
        mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) root.findPreference(KEY_ASSISTED_GPS);
        if (GoogleLocationSettingHelper.isAvailable(getActivity())) {
            // GSF present, Add setting for 'Use My Location'
            CheckBoxPreference useLocation = new CheckBoxPreference(getActivity());
            useLocation.setKey(KEY_USE_LOCATION);
            useLocation.setTitle(R.string.use_location_title);
            useLocation.setSummary(R.string.use_location_summary);
            useLocation.setChecked(
                    GoogleLocationSettingHelper.getUseLocationForServices(getActivity())
                    == GoogleLocationSettingHelper.USE_LOCATION_FOR_SERVICES_ON);
            useLocation.setPersistent(false);
            useLocation.setOnPreferenceChangeListener(this);
            getPreferenceScreen().addPreference(useLocation);
            mUseLocation = useLocation;
        }

        // Change the summary for wifi-only devices
        if (Utils.isWifiOnly(getActivity())) {
            mNetwork.setSummaryOn(R.string.location_neighborhood_level_wifi);
        }
        //add BT gps devices
        mGPSBTPref = (ListPreference) findPreference("location_gps_source");
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        for (String e : getResources().getStringArray(R.array.location_entries_gps_source) ) {
            entries.add(e);
        }
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();
        for (String v: getResources().getStringArray(R.array.location_values_gps_source)) {
            values.add(v);
        }
        // add known bonded BT devices
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ((mBluetoothAdapter != null) && (mBluetoothAdapter.isEnabled())) {
            for (BluetoothDevice d : mBluetoothAdapter.getBondedDevices()) {
                String dname = d.getName() + " - " + d.getAddress();
                entries.add(dname);
                values.add(d.getAddress());
            }
        }
        mGPSBTPref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mGPSBTPref.setEntryValues(values.toArray(new CharSequence[values.size()]));
        mGPSBTPref.setDefaultValue("0");
        mGPSBTPref.setOnPreferenceChangeListener(this);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        updateLocationToggles();

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
        }

        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mNetwork) {
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.GPS_PROVIDER, enabled);
            if (mAssistedGps != null) {
                mAssistedGps.setEnabled(enabled);
            }
        } else if (preference == mAssistedGps) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSISTED_GPS_ENABLED,
                    mAssistedGps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateLocationToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mNetwork.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(gpsEnabled);
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Secure.getInt(res,
                    Settings.Secure.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mUseLocation) {
            boolean newValue = (value == null ? false : (Boolean) value);
            GoogleLocationSettingHelper.setUseLocationForServices(getActivity(), newValue);
            // We don't want to change the value immediately here, since the user may click
            // disagree in the dialog that pops up. When the activity we just launched exits, this
            // activity will be restated and the new value re-read, so the checkbox will get its
            // new value then.
            return false;
        }  else if (preference == mGPSBTPref) {
            String oldPref = Settings.System.getString(getContentResolver(),
                    Settings.Secure.EXTERNAL_GPS_BT_DEVICE);
            String newPref = value == null ? "0" : (String) value;
            // "0" represents the internal GPS.
            Settings.System.putString(getContentResolver(), Settings.Secure.EXTERNAL_GPS_BT_DEVICE,
                    newPref);
            if (!oldPref.equals(newPref) && ("0".equals(oldPref) || "0".equals(newPref)) ) {
                LocationManager locationManager =
                        (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                locationManager.setGPSSource(newPref);
                // Show msg to inform user that source has been switched
                Toast.makeText(this.getActivity(),
                        getResources().getString(R.string.location_gps_source_notification),
                        Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }
}
