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

import android.content.Context;
import android.hardware.dumpstate.V1_1.IDumpstateDevice;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.NoSuchElementException;

public class EnableVendorLoggingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String ENABLE_VENDOR_LOGGING_KEY = "enable_vendor_logging";

    public EnableVendorLoggingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_VENDOR_LOGGING_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        setDeviceLoggingEnabled(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enableGpuDebugLayersMode = getDeviceLoggingEnabled();
        ((SwitchPreference) mPreference).setChecked(enableGpuDebugLayersMode);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        setDeviceLoggingEnabled(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    private void setDeviceLoggingEnabled(boolean enable) {
        try {
            IDumpstateDevice server = IDumpstateDevice.getService(true /*retry*/);
            server.setDeviceLoggingEnabled(enable);
        } catch (RemoteException re) {
            // ignore
        } catch(NoSuchElementException nsee) {
            // ignore again
        }
    }

    private boolean getDeviceLoggingEnabled() {
        try {
            IDumpstateDevice server = IDumpstateDevice.getService(true /*retry*/);
            return server.getDeviceLoggingEnabled();
        } catch (RemoteException re) {
            // ignore
        } catch(NoSuchElementException nsee) {
            // ignore again
        }
        return false;
    }
}
