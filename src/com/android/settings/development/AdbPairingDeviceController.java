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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.FooterPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for logic pertaining to displaying adb device information for the
 * {@link AdbPairingDeviceFragment}.
 */
public class AdbPairingDeviceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause,
        OnResume, AdbWirelessDialog.AdbWirelessDialogListener {

    private static final String TAG = "AdbPairingDeviceController";

    static final String DEVICE_ID_EXTRA = "device_id";

    @VisibleForTesting
    static final String KEY_DEVICE_NAME_PREF = "device_name_pref";
    @VisibleForTesting
    static final String KEY_PAIRING_PROGRESS_CATEGORY = "pairing_progress_category";

    private String mDeviceName;
    private PairDevice mSelectedPairingDevice;

    private final Fragment mFragment;
    private final IAdbManager mAdbManager;

    // UI elements - in order of appearance
    private Preference mDeviceNamePref;
    private AdbPairingDevicesProgressCategory mPairingProgressCategory;

    private final IconInjector mIconInjector;
    private AdbWirelessDialog mDialog;

    // A copy of the pairing devices delivered by the broadcast
    private HashMap<Integer, PairDevice> mPairingDevices;
    // map of the device id to preference
    private HashMap<Integer, Preference> mPreferenceMap;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION.equals(action)) {
                HashMap<Integer, PairDevice> newPairingDevicesList =
                        (HashMap<Integer, PairDevice>) intent.getSerializableExtra(
                            AdbManager.WIRELESS_DEVICES_EXTRA);
                updatePairingDevicePreferences(newPairingDevicesList);
            } else if (mDialog != null && AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION.equals(action)) {
                Log.i(TAG, "Got pairing result action");
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);

                if (res.equals(AdbManager.WIRELESS_STATUS_PAIRING_CODE)) {
                    Log.i(TAG, "Got pairing code extra");
                }

                PairDevice pd = (PairDevice) intent.getSerializableExtra(
                        AdbManager.WIRELESS_PAIR_DEVICE_EXTRA);
                if (pd == null || pd.getDeviceId() != mSelectedPairingDevice.getDeviceId()) {
                    // Ignore messages about other devices.
                    if (pd == null) {
                    Log.i(TAG, "Pair device is null");
                    } else {
                      Log.i(TAG, "pd={" + pd + "} selected=" + mSelectedPairingDevice + "}");
                    }
                    return;
                }

                if (res.equals(AdbManager.WIRELESS_STATUS_PAIRING_CODE)) {
                    Log.i(TAG, "Got pairing code: " + intent.getStringExtra(AdbManager.WIRELESS_PAIRING_CODE_EXTRA));
                    mDialog.getController().setPairingCode(
                            intent.getStringExtra(
                                AdbManager.WIRELESS_PAIRING_CODE_EXTRA));
                } else if (res.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
                    Log.i(TAG, "success");
                    mDialog.dismiss(res);
                } else if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    mDialog.dismiss(res);
                    Log.i(TAG, "fail");
                }
            }
        }
    };

    public static AdbPairingDeviceController newInstance(
            String deviceName,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle) {
        return new AdbPairingDeviceController(
                deviceName, context, fragment, lifecycle,
                new IconInjector(context));
    }

    @VisibleForTesting
        /* package */ AdbPairingDeviceController(
            String deviceName,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle,
            IconInjector injector) {
        super(context);

        mDeviceName = deviceName;
        mFragment = fragment;
        mIconInjector = injector;
        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
        mIntentFilter.addAction(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));

        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null since this controller contains more than one Preference
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mDeviceNamePref = (Preference) screen.findPreference(KEY_DEVICE_NAME_PREF);
        mDeviceNamePref.setSummary(mDeviceName);

        mPairingProgressCategory = (AdbPairingDevicesProgressCategory) screen.findPreference(KEY_PAIRING_PROGRESS_CATEGORY);
        mPairingProgressCategory.setProgress(true);
    }

    private void updatePairingDevicePreferences(HashMap<Integer, PairDevice> pairingDevices) {
        mPairingDevices = pairingDevices;

        if (mPairingDevices == null) {
            mPairingProgressCategory.removeAll();
            return;
        }
        if (mPreferenceMap == null) {
            mPreferenceMap = new HashMap<Integer, Preference>();
        }

        if (mPreferenceMap.isEmpty()) {
            for (Map.Entry<Integer, PairDevice> entry : mPairingDevices.entrySet()) {
                Preference p =
                        new Preference(mPairingProgressCategory.getContext());
                p.setTitle(entry.getValue().getDeviceName());
                p.setSummary(R.string.adb_pairing_device_pref_summary);
                mPreferenceMap.put(
                        entry.getKey(),
                        p);
                // Store the device id in the preference bundle so we can fetch it later.
                Bundle bundle = p.getExtras();
                bundle.putInt(DEVICE_ID_EXTRA, entry.getKey());
                p.setOnPreferenceClickListener(preference -> {
                    AdbPairingDeviceFragment f = (AdbPairingDeviceFragment) mFragment;
                    Integer deviceId = preference.getExtras().getInt(DEVICE_ID_EXTRA);
                    showDevicePairingDialog(mPairingDevices.get(deviceId));
                    return true;
                });
                mPairingProgressCategory.addPreference(p);
            }
        } else {
            // Remove any devices no longer on the mPairingDevices
            mPreferenceMap.entrySet().removeIf(entry -> {
                if (mPairingDevices.get(entry.getKey()) == null) {
                    mPairingProgressCategory.removePreference(entry.getValue());
                    return true;
                }
                return false;
            });
            // Add new devices if any.
            for (Map.Entry<Integer, PairDevice> entry :
                    mPairingDevices.entrySet()) {
                if (mPreferenceMap.get(entry.getKey()) == null) {
                    Preference p =
                            new Preference(mPairingProgressCategory.getContext());
                    p.setTitle(entry.getValue().getDeviceName());
                    p.setSummary(R.string.adb_pairing_device_pref_summary);
                    mPreferenceMap.put(
                            entry.getKey(),
                            p);
                    // Store the device id in the preference bundle so we can fetch it later.
                    Bundle bundle = p.getExtras();
                    bundle.putInt(DEVICE_ID_EXTRA, entry.getKey());
                    p.setOnPreferenceClickListener(preference -> {
                        AdbPairingDeviceFragment f = (AdbPairingDeviceFragment) mFragment;
                        Integer deviceId = preference.getExtras().getInt(DEVICE_ID_EXTRA);
                        showDevicePairingDialog(mPairingDevices.get(deviceId));
                        return true;
                    });
                    mPairingProgressCategory.addPreference(p);
                }
            }
        }
    }

    private void showDevicePairingDialog(PairDevice pairingDevice) {
        mSelectedPairingDevice = pairingDevice;
        AdbPairingDeviceFragment f = (AdbPairingDeviceFragment) mFragment;
        f.showDevicePairingDialog();
    }

    public Dialog createDialog(int dialogId) {
        switch (dialogId) {
            case AdbPairingDeviceFragment.PAIRING_DEVICE_DIALOG_ID:
                mDialog = AdbWirelessDialog.createModal(
                        mFragment.getActivity(),
                        this,
                        mSelectedPairingDevice,
                        AdbWirelessDialogUiBase.MODE_PAIRING);
                break;
            case AdbPairingDeviceFragment.PAIRING_DEVICE_FAILED_DIALOG_ID:
                mDialog = AdbWirelessDialog.createModal(
                        mFragment.getActivity(),
                        this,
                        mSelectedPairingDevice,
                        AdbWirelessDialogUiBase.MODE_PAIRING_FAILED);
                break;
        }
        return mDialog;
    }

    @Override
    public void onDismiss(PairDevice pairDevice, Integer result) {
        if (result.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
            // Go back to the WirelessDebugging page
            mFragment.getActivity().finish();
        } else {
            // Show failure dialog message for the device
            AdbPairingDeviceFragment f = (AdbPairingDeviceFragment) mFragment;
            f.showDevicePairingFailedDialog();
        }
        mDialog = null;
    }

    @Override
    public void onCancel(PairDevice pairDevice) {
        try {
            mAdbManager.cancelPairing(pairDevice.getDeviceId());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to cancel pairing");
        }

        mDialog = null;
    }

    @Override
    public void onResume() {
        mFragment.getActivity().registerReceiver(mReceiver, mIntentFilter);
        try {
            mAdbManager.queryAdbWirelessPairingDevices();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to request pairing list");
        }
    }

    @Override
    public void onPause() {
        mFragment.getActivity().unregisterReceiver(mReceiver);
    }

    /**
     * Wrapper for testing compatibility.
     */
    @VisibleForTesting
    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            mContext = context;
        }

        public Drawable getIcon(int level) {
            return mContext.getDrawable(Utils.getWifiIconResource(level)).mutate();
        }
    }
}


