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

import android.annotation.Nullable;
import android.content.Context;
import android.hardware.dumpstate.V1_1.IDumpstateDevice;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.NoSuchElementException;

public class EnableVendorLoggingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "EnableVendorLoggingPreferenceController";
    private static final String ENABLE_VENDOR_LOGGING_KEY = "enable_vendor_logging";

    public EnableVendorLoggingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_VENDOR_LOGGING_KEY;
    }

    @Override
    public boolean isAvailable() {
        // Only show preference when IDumpstateDevice v1.1 is avalaible
        // This is temperary strategy that may change later.
        return isIDumpstateDeviceV1_1ServiceAvailable();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        setDeviceLoggingEnabled(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = getDeviceLoggingEnabled();
        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        setDeviceLoggingEnabled(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    boolean isIDumpstateDeviceV1_1ServiceAvailable() {
        IDumpstateDevice server = getDumpstateDeviceService();
        if (server == null) {
            Log.d(TAG, "IDumpstateDevice v1.1 server is not available.");
        }
        return server != null;
    }

    @VisibleForTesting
    void setDeviceLoggingEnabled(boolean enable) {
        IDumpstateDevice server = getDumpstateDeviceService();
        try {
            server.setDeviceLoggingEnabled(enable);
        } catch (NullPointerException | RemoteException e) {
            Log.e(TAG, "setDeviceLoggingEnabled return: " + e);
        }
    }

    @VisibleForTesting
    boolean getDeviceLoggingEnabled() {
        IDumpstateDevice server = getDumpstateDeviceService();
        try {
            return server.getDeviceLoggingEnabled();
        } catch (NullPointerException | RemoteException e) {
            Log.e(TAG, "getDeviceLoggingEnabled return: " + e);
        }
        return false;
    }

    private @Nullable IDumpstateDevice getDumpstateDeviceService() {
        try {
            IDumpstateDevice server = IDumpstateDevice.getService(true /*retry*/);
            return server;
        } catch (RemoteException | NoSuchElementException e) {
            Log.e(TAG, "getDumpstateDeviceService return: " + e);
        }
        return null;
    }
}
