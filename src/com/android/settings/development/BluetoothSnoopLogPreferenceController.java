/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.sysprop.BluetoothProperties;
import androidx.annotation.VisibleForTesting;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothSnoopLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bt_hci_snoop_log";
    @VisibleForTesting

    public BluetoothSnoopLogPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        BluetoothProperties.btsnoop_enable((Boolean)newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean enableBtSnoopLog =
                BluetoothProperties.btsnoop_enable().orElse(false);
        ((SwitchPreference) mPreference).setChecked(enableBtSnoopLog);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        BluetoothProperties.btsnoop_enable(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
