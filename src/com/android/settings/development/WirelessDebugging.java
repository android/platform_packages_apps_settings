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
import android.debug.AdbManager;
import android.debug.IAdbManager;
import android.debug.PairDevice;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
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

@SearchIndexable
public class WirelessDebugging extends DashboardFragment
        implements Indexable, WirelessDebuggingEnabler.OnEnabledListener {

    private final String TAG = this.getClass().getSimpleName();

    // Activity result from clicking on a paired device.
    private static final int PAIRED_DEVICE_REQUEST = 0;
    public static final String PAIRED_DEVICE_REQUEST_TYPE = "request_type";
    public static final int FORGET_ACTION = 0;
    public static final String PAIRED_DEVICE_EXTRA = "paired_device";
    public static final String DEVICE_NAME_EXTRA = "device_name";

    private WirelessDebuggingEnabler mWifiDebuggingEnabler;

    // UI components
    private static final String PREF_KEY_ADB_DEVICE_NAME = "adb_device_name_pref";
    private static final String PREF_KEY_PAIRING_METHODS_CATEGORY = "adb_pairing_methods_category";
    private static final String PREF_KEY_ADB_CODE_PAIRING = "adb_pair_method_code_pref";
    private static final String PREF_KEY_PAIRED_DEVICES_CATEGORY = "adb_paired_devices_category";
    private static final String PREF_KEY_FOOTER_CATEGORY = "adb_wireless_footer_category";

    private ValidatedEditTextPreference mDeviceNamePreference;
    private AdbDeviceNameTextValidator mDeviceNameValidator;

    private PreferenceCategory mPairingMethodsCategory;
    private Preference mCodePairingPreference;

    private PreferenceCategory mPairedDevicesCategory;

    private PreferenceCategory mFooterCategory;
    private FooterPreference mOffMessagePreference;

    // map of paired devices, with the device id as the key
    private HashMap<String, AdbPairedDevicePreference> mPairedDevicePreferences;

    private final IAdbManager mAdbManager;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION.equals(action)) {
                HashMap<String, PairDevice> newPairedDevicesList =
                        (HashMap<String, PairDevice>) intent.getSerializableExtra(
                            AdbManager.WIRELESS_DEVICES_EXTRA);
                updatePairedDevicePreferences(newPairedDevicesList);
            }
        }
    };

    public WirelessDebugging() {
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
    }

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
        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION);
    }

    private void addPreferences() {
        mDeviceNamePreference =
            (ValidatedEditTextPreference) findPreference(PREF_KEY_ADB_DEVICE_NAME);
        mPairingMethodsCategory =
                (PreferenceCategory) findPreference(PREF_KEY_PAIRING_METHODS_CATEGORY);
        mCodePairingPreference =
                (Preference) findPreference(PREF_KEY_ADB_CODE_PAIRING);
        mCodePairingPreference.setOnPreferenceClickListener(preference -> {
            launchDevicePairingFragment();
            return true;
        });

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
        try {
            mDeviceNamePreference.setSummary(mAdbManager.getName());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the device name for ADB wireless");
        }
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
        try {
            mAdbManager.queryAdbWirelessPairedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to request the paired list for Adb wireless");
        } finally {
            if (mWifiDebuggingEnabler != null) {
                mWifiDebuggingEnabler.resume(activity);
            }
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

        if (requestCode == PAIRED_DEVICE_REQUEST) {
            handlePairedDeviceRequest(resultCode, data);
        }
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

    private void updatePairedDevicePreferences(HashMap<String, PairDevice> newList) {
        // TODO(joshuaduong): Move the non-UI stuff into another thread
        // as the processing could take some time.
        if (newList == null) {
            mPairedDevicesCategory.removeAll();
            return;
        }
        if (mPairedDevicePreferences == null) {
            mPairedDevicePreferences = new HashMap<String, AdbPairedDevicePreference>();
        }
        if (mPairedDevicePreferences.isEmpty()) {
            for (Map.Entry<String, PairDevice> entry : newList.entrySet()) {
                AdbPairedDevicePreference p =
                        new AdbPairedDevicePreference(entry.getValue(),
                            mPairedDevicesCategory.getContext());
                mPairedDevicePreferences.put(
                        entry.getKey(),
                        p);
                p.setOnPreferenceClickListener(preference -> {
                    AdbPairedDevicePreference pref =
                        (AdbPairedDevicePreference) preference;
                    launchPairedDeviceDetailsFragment(pref);
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
                    // It is in the newList. Just update the PairDevice value
                    AdbPairedDevicePreference p =
                            entry.getValue();
                    p.setPairedDevice(newList.get(entry.getKey()));
                    p.refresh();
                    return false;
                }
            });
            // Add new devices if any.
            for (Map.Entry<String, PairDevice> entry :
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
                        launchPairedDeviceDetailsFragment(pref);
                        return true;
                    });
                    mPairedDevicesCategory.addPreference(p);
                }
            }
        }
    }

    private void launchPairedDeviceDetailsFragment(AdbPairedDevicePreference p) {
        // For sending to the device details fragment.
        p.savePairedDeviceToExtras(p.getExtras());
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.adb_wireless_device_details_title)
                .setDestination(AdbDeviceDetailsFragment.class.getName())
                .setArguments(p.getExtras())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, PAIRED_DEVICE_REQUEST)
                .launch();
    }

    void handlePairedDeviceRequest(int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }

        Log.i(TAG, "Processing paired device request");
        int requestType = data.getIntExtra(PAIRED_DEVICE_REQUEST_TYPE, -1);

        PairDevice p;

        switch (requestType) {
            case FORGET_ACTION:
                try {
                    p = (PairDevice) data.getSerializableExtra(PAIRED_DEVICE_EXTRA);
                          mAdbManager.unpairDevice(p.getGuid());
                    mPairedDevicesCategory.removePreference(
                        mPairedDevicePreferences.get(p.getGuid()));
                    mPairedDevicePreferences.remove(p.getGuid());
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to forget the device");
                }
                break;
            default:
                break;
        }
    }

    private void launchDevicePairingFragment() {
        // For sending to the pairing device fragment.
        try {
            Bundle bundle = new Bundle();
            bundle.putCharSequence(DEVICE_NAME_EXTRA,
                    mAdbManager.getName());
            new SubSettingLauncher(getContext())
                    .setTitleRes(R.string.adb_pair_new_devices_title)
                    .setDestination(AdbPairingDeviceFragment.class.getName())
                    .setArguments(bundle)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .launch();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the device name");
        }
    }

}
