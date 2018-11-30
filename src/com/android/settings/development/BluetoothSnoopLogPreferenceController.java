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
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothSnoopLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bt_hci_snoop_log";
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_DISABLED_INDEX = 0;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_FILTERED_INDEX = 1;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_FULL_INDEX = 2;
    // Default mode is FILTERED on userdebug/eng build, DISABLED on user build
    @VisibleForTesting
    static final int DEFAULT_BTSNOOP_LOG_MODE_INDEX =
            Build.IS_DEBUGGABLE ? BTSNOOP_LOG_MODE_FILTERED_INDEX : BTSNOOP_LOG_MODE_DISABLED_INDEX;
    @VisibleForTesting
    static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY =
            "persist.bluetooth.btsnoopenable";

    private final String[] mListValues;
    private final String[] mListEntries;

    public BluetoothSnoopLogPreferenceController(Context context) {
        super(context);
        mListValues = context.getResources().getStringArray(R.array.bt_hci_snoop_log_values);
        mListEntries = context.getResources().getStringArray(R.array.bt_hci_snoop_log_entries);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, newValue.toString());
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        String currentValue = SystemProperties.get(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY);
        // Translate legacy mode (until Android P) into new ones
        if (TextUtils.isEmpty(currentValue)) {
            currentValue = mListValues[DEFAULT_BTSNOOP_LOG_MODE_INDEX];
        } else if (TextUtils.equals(currentValue, Boolean.toString(true))) {
            currentValue = mListValues[BTSNOOP_LOG_MODE_FULL_INDEX];
            SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, currentValue);
        } else if (TextUtils.equals(currentValue, Boolean.toString(false))) {
            currentValue = mListValues[DEFAULT_BTSNOOP_LOG_MODE_INDEX];
            SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, currentValue);
        }
        // Verify that currentValue actually matches an item in mListValues in normal case
        int index = DEFAULT_BTSNOOP_LOG_MODE_INDEX;
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListEntries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, null);
        ((ListPreference) mPreference).setValue(mListValues[DEFAULT_BTSNOOP_LOG_MODE_INDEX]);
        ((ListPreference) mPreference).setSummary(mListEntries[DEFAULT_BTSNOOP_LOG_MODE_INDEX]);
    }
}
