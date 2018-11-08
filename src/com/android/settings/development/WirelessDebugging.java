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
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

@SearchIndexable
public class WirelessDebugging extends DashboardFragment
        implements Indexable, WirelessDebuggingEnabler.OnEnabledListener {

    private final String TAG = this.getClass().getSimpleName();
    private WirelessDebuggingEnabler mWifiDebuggingEnabler;
    private PreferenceCategory mStatusCategory;

    // UI components
    private static final String PREF_KEY_FOOTER_CATEGORY = "adb_wireless_footer_category";
    private PreferenceCategory mFooterCategory;
    private FooterPreference mOffMessagePreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mWifiDebuggingEnabler =  new WirelessDebuggingEnabler(activity,
                                                              new SwitchBarController(activity.getSwitchBar()),
                                                              this);
    }

    private void addPreferences() {
        mFooterCategory =
                (PreferenceCategory) findPreference(PREF_KEY_FOOTER_CATEGORY);

        mOffMessagePreference =
                new FooterPreference(mFooterCategory.getContext());
        final CharSequence title = getText(R.string.adb_wireless_list_empty_off);
        mOffMessagePreference.setTitle(title);
        mFooterCategory.addPreference(mOffMessagePreference);

        final CharSequence deviceNameTitle =
                getText(R.string.my_device_info_device_name_preference_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWifiDebuggingEnabler.teardownSwitchController();
        mWifiDebuggingEnabler = null;
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        mWifiDebuggingEnabler.resume(activity);
    }

    @Override
    public void onPause() {
        super.onPause();

        mWifiDebuggingEnabler.pause();
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
       return R.xml.adb_wireless_settings;
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

    private void showOffMessage() {
        mFooterCategory.setVisible(true);
    }

    private void showDebuggingPreferences() {
        mFooterCategory.setVisible(false);
    }
}
