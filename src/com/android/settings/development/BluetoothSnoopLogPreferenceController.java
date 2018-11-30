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
    static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY =
            "persist.bluetooth.btsnoopenable";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public BluetoothSnoopLogPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.bt_hci_snoop_log_values);
        mListSummaries = context.getResources().getStringArray(R.array.bt_hci_snoop_log_entries);
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
        final String currentValue = SystemProperties.get(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY);
        int index = 0; // Defaults to Filtered Snoop Logs
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, mListValues[0]);
        ((ListPreference) mPreference).setValue(mListValues[0]);
        ((ListPreference) mPreference).setSummary(mListSummaries[0]);
    }
}
