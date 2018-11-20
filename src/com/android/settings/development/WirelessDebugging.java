/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

@SearchIndexable
public class WirelessDebugging extends DashboardFragment
        implements Indexable, WirelessDebuggingEnabler.OnEnabledListener {

    private final String TAG = this.getClass().getSimpleName();

    private WirelessDebuggingEnabler mWifiDebuggingEnabler;

    // UI components
    private static final String PREF_KEY_ADB_DEVICE_NAME = "adb_device_name_pref";
    private static final String PREF_KEY_PAIRING_METHODS_CATEGORY = "adb_pairing_methods_category";
    private static final String PREF_KEY_ADB_CODE_PAIRING = "adb_pair_method_code_pref";
    private static final String PREF_KEY_PAIRED_DEVICES_CATEGORY = "adb_paired_devices_category";
    private static final String PREF_KEY_FOOTER_CATEGORY = "adb_wireless_footer_category";

    private String mDeviceName;
    private String mPendingDeviceName;

    private ValidatedEditTextPreference mDeviceNamePreference;
//    private AdbDeviceNameTextValidator mDeviceNameValidator;

    private PreferenceCategory mPairingMethodsCategory;
    private Preference mCodePairingPreference;

    private PreferenceCategory mPairedDevicesCategory;

    private PreferenceCategory mFooterCategory;
    private FooterPreference mOffMessagePreference;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION.equals(action)) {
                Log.i(TAG, "Got the paired list, TODO need to show it");
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mWifiDebuggingEnabler =  new WirelessDebuggingEnabler(activity,
                                                              new SwitchBarController(activity.getSwitchBar()),
                                                              this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferences();
        mIntentFilter = new IntentFilter(WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION);
    }

    private void addPreferences() {
        mDeviceNamePreference =
            (ValidatedEditTextPreference) findPreference(PREF_KEY_ADB_DEVICE_NAME);
        mPairingMethodsCategory =
                (PreferenceCategory) findPreference(PREF_KEY_PAIRING_METHODS_CATEGORY);
        mCodePairingPreference =
                (Preference) findPreference(PREF_KEY_ADB_CODE_PAIRING);
        mPairedDevicesCategory =
                (PreferenceCategory) findPreference(PREF_KEY_PAIRED_DEVICES_CATEGORY);
        mFooterCategory =
                (PreferenceCategory) findPreference(PREF_KEY_FOOTER_CATEGORY);

        mOffMessagePreference =
                new FooterPreference(mFooterCategory.getContext());
        final CharSequence title = getText(R.string.adb_wireless_list_empty_off);
        mOffMessagePreference.setTitle(title);
        mFooterCategory.addPreference(mOffMessagePreference);

        final CharSequence deviceNameTitle =
                getText(R.string.my_device_info_device_name_preference_title);
        mDeviceNamePreference.setTitle(deviceNameTitle);
        mDeviceNamePreference.setSummary(
                WirelessDebuggingManager.getInstance(
                    getActivity().getApplicationContext()).getName());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWifiDebuggingEnabler.teardownSwitchController();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        getActivity().registerReceiver(mReceiver, mIntentFilter);
        mWifiDebuggingEnabler.resume(activity);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
        mWifiDebuggingEnabler.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_ADB_WIRELESS;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
       return R.xml.adb_wireless_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onEnabled(boolean enabled) {
        if (enabled) {
            showDebuggingPreferences();
        } else {
            showOffMessage();
        }
    }

    private void showOffMessage() {
        Log.i(TAG, "setOffMassage()");
        mDeviceNamePreference.setVisible(false);
        mPairingMethodsCategory.setVisible(false);
        mPairedDevicesCategory.setVisible(false);
        mFooterCategory.setVisible(true);
    }

    private void showDebuggingPreferences() {
        Log.i(TAG, "updateDeviceName()");
        mDeviceNamePreference.setVisible(true);
        mPairingMethodsCategory.setVisible(true);
        mPairedDevicesCategory.setVisible(true);
        mFooterCategory.setVisible(false);
    }
}
