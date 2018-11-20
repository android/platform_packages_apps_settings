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
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO(joshuaduong): remove once AdbWirelessManager is in.
import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.WirelessDebuggingManager.PairedDevice;

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
    private AdbDeviceNameTextValidator mDeviceNameValidator;

    private PreferenceCategory mPairingMethodsCategory;
    private Preference mCodePairingPreference;

    private PreferenceCategory mPairedDevicesCategory;

    private PreferenceCategory mFooterCategory;
    private FooterPreference mOffMessagePreference;

    // map of paired devices, with the device id as the key
    private HashMap<Integer, AdbPairedDevicePreference> mPairedDevicePreferences;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION.equals(action)) {
                HashMap<Integer, PairedDevice> newPairedDevicesList =
                        (HashMap<Integer, PairedDevice>) intent.getSerializableExtra(
                            WirelessDebuggingManager.DEVICE_LIST_EXTRA);
                updatePairedDevicePreferences(newPairedDevicesList);
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mWifiDebuggingEnabler = createWirelessDebuggingEnabler();
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        getActivity().registerReceiver(mReceiver, mIntentFilter);
        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.pause();
        }
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context,getActivity(), this /* fragment */,
                getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Activity activity, WirelessDebugging fragment,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        AdbDeviceNamePreferenceController adbDeviceNamePreferenceController =
                new AdbDeviceNamePreferenceController(context);
        if (lifecycle != null) {
            lifecycle.addObserver(adbDeviceNamePreferenceController);
        }
        controllers.add(adbDeviceNamePreferenceController);
        return controllers;
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

    /**
     * @return new WirelessDebuggingEnabler or null
     */
    private WirelessDebuggingEnabler createWirelessDebuggingEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        return new WirelessDebuggingEnabler(activity,
                        new SwitchBarController(activity.getSwitchBar()), this);
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

    private void updatePairedDevicePreferences(HashMap<Integer, PairedDevice> newList) {
        Log.i(TAG, "New paired device list: " + newList);
        // TODO(joshuaduong): Move the non-UI stuff into another thread
        // as the processing could take some time.
        if (mPairedDevicePreferences == null) {
            mPairedDevicePreferences = new HashMap<Integer, AdbPairedDevicePreference>();
        }
        if (mPairedDevicePreferences.isEmpty()) {
            for (Map.Entry<Integer, PairedDevice> entry : newList.entrySet()) {
                AdbPairedDevicePreference p =
                        new AdbPairedDevicePreference(entry.getValue(),
                            mPairedDevicesCategory.getContext());
                mPairedDevicePreferences.put(
                        entry.getKey(),
                        p);
                p.setOnPreferenceClickListener(preference -> {
                    AdbPairedDevicePreference pref =
                        (AdbPairedDevicePreference) preference;
                    PairedDevice pairedDevice = pref.getPairedDevice();
                    Log.i(TAG, "OnClick for " + pairedDevice.getDeviceName());
                    return true;
                });
                mPairedDevicesCategory.addPreference(p);
            }
        } else {
            // Remove any devices no longer on the newList
            mPairedDevicePreferences.entrySet().removeIf(entry -> {
                if (newList.get(entry.getKey()) == null) {
                    mPairedDevicesCategory.removePreference(entry.getValue());
                    return true;
                } else {
                    // It is in the newList. Just update the PairedDevice value
                    AdbPairedDevicePreference p =
                            entry.getValue();
                    p.setPairedDevice(newList.get(entry.getKey()));
                    p.refresh();
                    return false;
                }
            });
            // Add new devices if any.
            for (Map.Entry<Integer, PairedDevice> entry :
                    newList.entrySet()) {
                if (mPairedDevicePreferences.get(entry.getKey()) == null) {
                    AdbPairedDevicePreference p =
                            new AdbPairedDevicePreference(entry.getValue(),
                                mPairedDevicesCategory.getContext());
                    mPairedDevicePreferences.put(
                            entry.getKey(),
                            p);
                    p.setOnPreferenceClickListener(preference -> {
                        AdbPairedDevicePreference pref =
                            (AdbPairedDevicePreference) preference;
                        PairedDevice pairedDevice = pref.getPairedDevice();
                        Log.i(TAG, "OnClick for " + pairedDevice.getDeviceName());
                        return true;
                    });
                    mPairedDevicesCategory.addPreference(p);
                }
            }
        }
    }
}
