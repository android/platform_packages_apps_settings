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
import android.provider.Settings;
// TODO: cken@: do we need test?
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class NetworkDnsResolverLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "NetworkDnsResolverLogPreferenceController";
    private static final String NW_DNS_RESOLVER_LOG_KEY = "nw_dns_resolver_log";

    @VisibleForTesting
    static final String NW_DNS_RESOLVER_LOG_PROPERTY = "persist.sys.nw_dns_resolver_log";
    @VisibleForTesting
    static final String USER_BUILD_TYPE = "user";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public NetworkDnsResolverLogPreferenceController(Context context) {
        super(context);

        mListValues = mContext.getResources().getStringArray(R.array.nw_dns_resolver_log_values);
        mListSummaries = mContext.getResources().getStringArray(R.array.nw_dns_resolver_log_summaries);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.equals(USER_BUILD_TYPE, getBuildType());
    }

    @Override
    public String getPreferenceKey() {
        return NW_DNS_RESOLVER_LOG_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(NW_DNS_RESOLVER_LOG_PROPERTY, newValue.toString());
        updateDnsResolverLogValues((ListPreference) mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateDnsResolverLogValues((ListPreference) mPreference);
    }

    private void updateDnsResolverLogValues(ListPreference preference) {
        final String currentValue = SystemProperties.get(NW_DNS_RESOLVER_LOG_PROPERTY);
        int index = 5; // Defaults to 'off'. Needs to match with R.array.nw_dns_resolver_log_values
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                Settings.Global.putString(mContext.getContentResolver(),
                                          ettings.Global.DNS_RESOLVER_LOG, currentValue);
                index = i;
                break;
            }
        }

        preference.setValue(mListValues[index]);
        preference.setSummary(mListSummaries[index]);
    }

    @VisibleForTesting
    public String getBuildType() {
        return Build.TYPE;
    }
}
