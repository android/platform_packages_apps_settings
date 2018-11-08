/*
 * Copyright (C) 2020 The Android Open Source Project
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
    private String mIpAddr;
    private String mPairingCode;
    private boolean mDiscoveryFailed;

    private final Fragment mFragment;
    private final IAdbManager mAdbManager;

    // UI elements - in order of appearance
    private Preference mDeviceNamePref;
    private AdbPairingDevicesProgressCategory mPairingProgressCategory;

    private final IconInjector mIconInjector;
    private AdbWirelessDialog mDialog;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);

                if (res.equals(AdbManager.WIRELESS_STATUS_PAIRING_CODE)) {
                    mPairingCode = intent.getStringExtra(
                                AdbManager.WIRELESS_PAIRING_CODE_EXTRA);
                } else if (res.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
                    if (mDialog != null) {
                        mDialog.dismiss(res);
                    } else {
                        mFragment.getActivity().finish();
                    }
                } else if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    if (mDialog != null) {
                        mDialog.dismiss(res);
                    } else {
                        Intent i = new Intent();
                        i.putExtra(
                                WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST_TYPE,
                                WirelessDebuggingFragment.FAIL_ACTION);
                        mFragment.getActivity().setResult(Activity.RESULT_OK, i);
                        mFragment.getActivity().finish();
                    }
                } else if (res.equals(AdbManager.WIRELESS_STATUS_CONNECTED)) {
                    int port = intent.getIntExtra(AdbManager.WIRELESS_DEBUG_PORT_EXTRA, 0);
                    Log.i(TAG, "Got pairing code port=" + port);
                    mIpAddr += ":" + port;
                    showDevicePairingDialog();
                }
            }
        }
    };

    public static AdbPairingDeviceController newInstance(
            String deviceName,
            String ipAddr,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle) {
        return new AdbPairingDeviceController(
                deviceName, ipAddr, context, fragment, lifecycle,
                new IconInjector(context));
    }

    @VisibleForTesting
        /* package */ AdbPairingDeviceController(
            String deviceName,
            String ipAddr,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle,
            IconInjector injector) {
        super(context);

        mDiscoveryFailed = false;
        mDeviceName = deviceName;
        mIpAddr = ipAddr;
        mFragment = fragment;
        mIconInjector = injector;
        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);

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

        mPairingProgressCategory = (AdbPairingDevicesProgressCategory) screen.findPreference(
                KEY_PAIRING_PROGRESS_CATEGORY);
        mPairingProgressCategory.setProgress(true);
    }

    private void showDevicePairingDialog() {
        AdbPairingDeviceFragment f = (AdbPairingDeviceFragment) mFragment;
        f.showDevicePairingDialog();
    }

    public Dialog createDialog(int dialogId) {
        switch (dialogId) {
            case AdbPairingDeviceFragment.PAIRING_DEVICE_DIALOG_ID:
                mDialog = AdbWirelessDialog.createModal(
                        mFragment.getActivity(),
                        this,
                        AdbWirelessDialogUiBase.MODE_PAIRING);
                mDialog.setPairingCode(mPairingCode);
                mDialog.setIpAddr(mIpAddr);
                break;
        }
        return mDialog;
    }

    @Override
    public void onDismiss(Integer result) {
        if (result.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
            Intent i = new Intent();
            i.putExtra(
                    WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST_TYPE,
                    mDiscoveryFailed ? WirelessDebuggingFragment.DISCOVERY_FAIL_ACTION
                    : WirelessDebuggingFragment.FAIL_ACTION);
            mFragment.getActivity().setResult(Activity.RESULT_OK, i);
            mFragment.getActivity().finish();
        } else {
            // Go back to the WirelessDebuggingFragment page
            mFragment.getActivity().finish();
        }
        mDialog = null;
    }

    @Override
    public void onCancel() {
        try {
            mAdbManager.disablePairing();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to cancel pairing");
        }

        mDialog = null;
    }

    @Override
    public void onResume() {
        mFragment.getActivity().registerReceiver(mReceiver, mIntentFilter);
        try {
            mAdbManager.enablePairingByPairingCode();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to enable pairing");
            mFragment.getActivity().finish();
        }
    }

    @Override
    public void onPause() {
        mFragment.getActivity().unregisterReceiver(mReceiver);
        try {
            mAdbManager.disablePairing();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to cancel pairing");
        }
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


