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

import android.content.Context;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

// test code
import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.Constants;

public class AdbDeviceNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
        Preference.OnPreferenceChangeListener,
        LifecycleObserver,
        OnSaveInstanceState,
        OnCreate {
    private static final String TAG = "AdbDeviceNamePreferenceController";

    private static final String PREF_KEY = "adb_device_name_pref";
    public static final int DEVICE_NAME_SET_WARNING_ID = 1;
    private static final String KEY_PENDING_DEVICE_NAME = "key_pending_device_name";
    private String mDeviceName;
    private ValidatedEditTextPreference mPreference;
    private final IAdbManager mAdbManager;

    public AdbDeviceNamePreferenceController(Context context) {
        super(context, PREF_KEY);

        if (Constants.USE_SIMULATION) {
            mAdbManager = WirelessDebuggingManager.getInstance(mContext.getApplicationContext());
        } else {
            mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
        }
        initializeDeviceName();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (ValidatedEditTextPreference) screen.findPreference(PREF_KEY);
        final CharSequence deviceName = getSummary();
        if (deviceName != null) {
            mPreference.setSummary(deviceName);
            mPreference.setText(deviceName.toString());
        }
        mPreference.setValidator(this);
    }

    private void initializeDeviceName() {
        try {
            mDeviceName = mAdbManager.getName();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the device name");
        }
    }

    @Override
    public CharSequence getSummary() {
        return mDeviceName;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String val = (String) newValue;
        if (val != null) {
            setDeviceName(val);
        }
        return true;
    }

    @Override
    public boolean isTextValid(String deviceName) {
        // TODO(joshuaduong): Find out from adbd what a valid name is. Just checking
        // if the name is non-empty for now.
        return deviceName != null && !deviceName.trim().isEmpty();
    }

    /**
     * This method presumes that security/validity checks have already been passed.
     */
    private void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
        mPreference.setSummary(getSummary());
        try {
            mAdbManager.setName(deviceName);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set the device name");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }
}
