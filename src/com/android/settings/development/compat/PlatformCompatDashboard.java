/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.compat;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes.REQUEST_COMPAT_APP;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.compat.Compatibility.ChangeConfig;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.IPlatformCompat;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.AppPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Dashboard for Platform Compat preferences.
 */
public class PlatformCompatDashboard extends DashboardFragment {

    private static final String TAG = "PlatformCompatDashboard";
    private static final String COMPAT_APP = "compat_app";

    private IPlatformCompat mPlatformCompat;
    private PackageManager mPackageManager;
    private CompatibilityChangeInfo[] mChanges;
    private String mSelectedApp;

    @Override
    public int getMetricsCategory() {
        return 42;//SettingsEnums.SETTINGS_PLATFORM_COMPAT_DASHBOARD;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.platform_compat_settings;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPlatformCompat = IPlatformCompat.Stub
                .asInterface(ServiceManager.getService(Context.PLATFORM_COMPAT_SERVICE));
        mPackageManager = getPackageManager();
        try {
            mChanges = mPlatformCompat.listAllChanges();
        } catch (RemoteException e) {
            throw new RuntimeException("Could not list changes!", e);
        }
        startAppPicker();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(COMPAT_APP, mSelectedApp);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMPAT_APP) {
            if (resultCode == Activity.RESULT_OK) {
                mSelectedApp = data.getAction();
                addPreferences();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private CompatibilityChangeConfig getAppChangeMappings() {
        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(mSelectedApp, 0);
            return mPlatformCompat.getAppConfig(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get ApplicationInfo for selected app!", e);
        } catch (RemoteException e) {
            throw new RuntimeException("Could not get app config!", e);
        }
    }

    private Preference createPreferenceForChange(Context context, CompatibilityChangeInfo change,
            CompatibilityChangeConfig configMappings) {
        boolean currentValue = configMappings.isChangeEnabledOrDefault(change.getId(), true);
        SwitchPreference item = new SwitchPreference(context);
        String changeName = change.getName() != null ? change.getName() : "Change_" + change.getId();
        item.setSummary(changeName);
        item.setKey(changeName);
        item.setEnabled(true);
        item.setDefaultValue(currentValue);
        item.setOnPreferenceChangeListener(
                new CompatChangePreferenceChangeListener(change.getId()));
        return item;
    }

    private void addAppPreference() {
        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(mSelectedApp, 0);
            Preference appPreference = new Preference(getPreferenceScreen().getContext());
            appPreference.setIcon(applicationInfo.loadIcon(mPackageManager));
            appPreference.setSummary(mSelectedApp
                    + " target sdk "
                    + applicationInfo.targetSdkVersion);
            appPreference.setOnPreferenceClickListener(
                    preference -> {
                        startAppPicker();
                        return true;
                    });
            getPreferenceScreen().addPreference(appPreference);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get ApplicationInfo for selected app!", e);
        }
    }

    private void addChangePreferencesToCategory(List<CompatibilityChangeInfo> changes,
            PreferenceCategory category, CompatibilityChangeConfig configMappings) {
        for (CompatibilityChangeInfo change : changes) {
            Preference preference = createPreferenceForChange(getPreferenceScreen().getContext(),
                    change, configMappings);
            category.addPreference(preference);
        }
    }

    private void addDefaultEnabledChanges(List<CompatibilityChangeInfo> changes,
            CompatibilityChangeConfig configMappings) {
        PreferenceCategory defaultEnabled =
                new PreferenceCategory(getPreferenceScreen().getContext());
        defaultEnabled.setTitle(R.string.platform_compat_default_enabled_title);
        getPreferenceScreen().addPreference(defaultEnabled);
        addChangePreferencesToCategory(changes, defaultEnabled, configMappings);
    }

    private void addDefaultDisabledChanges(List<CompatibilityChangeInfo> changes,
            CompatibilityChangeConfig configMappings) {
        PreferenceCategory defaultDisabled =
                new PreferenceCategory(getPreferenceScreen().getContext());
        defaultDisabled.setTitle(R.string.platform_compat_default_disabled_title);
        getPreferenceScreen().addPreference(defaultDisabled);
        addChangePreferencesToCategory(changes, defaultDisabled,
                configMappings);
    }

    private void addTargetSdkChanges(int targetSdk, List<CompatibilityChangeInfo> changes,
            CompatibilityChangeConfig configMappings) {
        PreferenceCategory targetSdkChanges =
                new PreferenceCategory(getPreferenceScreen().getContext());
        targetSdkChanges.setTitle("Enabled after SDK " + targetSdk);
        getPreferenceScreen().addPreference(targetSdkChanges);
        addChangePreferencesToCategory(changes, targetSdkChanges,
                configMappings);
    }

    private void addPreferences() {
        getPreferenceScreen().removeAll();
        addAppPreference();
        // Differentiate compatibility changes into default enabled, default disabled and enabled
        // after target sdk.
        CompatibilityChangeConfig configMappings = getAppChangeMappings();
        List<CompatibilityChangeInfo> enabledChanges = new ArrayList<>();
        List<CompatibilityChangeInfo> disabledChanges = new ArrayList<>();
        Map<Integer, List<CompatibilityChangeInfo>> targetSdkChanges = new TreeMap<>();
        for (CompatibilityChangeInfo change : mChanges) {
            if (change.getEnableAfterTargetSdk() != 0) {
                List<CompatibilityChangeInfo> sdkChanges;
                if (!targetSdkChanges.containsKey(change.getEnableAfterTargetSdk())) {
                    sdkChanges = new ArrayList<>();
                    targetSdkChanges.put(change.getEnableAfterTargetSdk(), sdkChanges);
                } else {
                    sdkChanges = targetSdkChanges.get(change.getEnableAfterTargetSdk());
                }
                sdkChanges.add(change);
            } else if (change.getDisabled()) {
                disabledChanges.add(change);
            } else {
                enabledChanges.add(change);
            }
        }
        addDefaultEnabledChanges(enabledChanges, configMappings);
        addDefaultDisabledChanges(disabledChanges, configMappings);
        for (Integer sdk : targetSdkChanges.keySet()) {
            addTargetSdkChanges(sdk, targetSdkChanges.get(sdk), configMappings);
        }
    }

    private void startAppPicker() {
        Intent intent = new Intent(getContext(), AppPicker.class);
        startActivityForResult(intent, REQUEST_COMPAT_APP);
    }

    private class CompatChangePreferenceChangeListener implements OnPreferenceChangeListener {
        private final long changeId;

        CompatChangePreferenceChangeListener(long changeId) {
            this.changeId = changeId;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                ArraySet<Long> enabled = new ArraySet<>();
                ArraySet<Long> disabled = new ArraySet<>();
                if ((Boolean) newValue) {
                    enabled.add(changeId);
                } else {
                    disabled.add(changeId);
                }
                CompatibilityChangeConfig overrides =
                        new CompatibilityChangeConfig(new ChangeConfig(enabled, disabled));
                mPlatformCompat.setOverrides(overrides, mSelectedApp);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
