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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.compat.CompatChangeInfo;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.compat.ParcelableCompatibilityChangeConfig;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.AppPicker;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Dashboard for Platform Compat preferences.
 */
public class PlatformCompatDashboard extends DashboardFragment {

    private static final String TAG = "PlatformCompatDashboard";
    private static final String COMPAT_APP = "compat_app";
    private IPlatformCompat mPlatformCompat;
    private PackageManager mPackageManager;
    private CompatChangeInfo[] mChanges;

    private String mSelectedApp;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_PLATFORM_COMPAT_DASHBOARD;
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
        if (savedInstanceState != null) {
            mSelectedApp = savedInstanceState.getString(COMPAT_APP);
        }
        mPlatformCompat = IPlatformCompat.Stub
                .asInterface(ServiceManager.getService(Context.PLATFORM_COMPAT_SERVICE));
        mPackageManager = getPackageManager();
        try {
            mChanges = mPlatformCompat.listAllChanges();
        } catch (RemoteException e) {
            throw new RuntimeException("Could not list changes!", e);
        }
        if (mSelectedApp == null) {
            startAppPicker();
        }
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
                populatePreferences();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Preference createPreferenceForChange(Context context, CompatChangeInfo change,
            Map<Long, Boolean> configMappings) {
        boolean currentValue = configMappings.getOrDefault(change.changeId, true);
        SwitchPreference item = new SwitchPreference(context);
        item.setSummary(change.name != null ? change.name : "Change " + change.changeId);
        item.setKey(change.name != null ? change.name : ("" + change.changeId));
        item.setEnabled(true);
        item.setDefaultValue(currentValue);
        item.setOnPreferenceChangeListener(
                new CompatChangePreferenceChangeListener(change.changeId));
        return item;
    }

    private void populatePreferences() {
        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(mSelectedApp, 0);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Map<Long, Boolean> configMappings = mPlatformCompat.getAppConfig(applicationInfo);
            preferenceScreen.removeAll();
            Preference appPreference = new Preference(preferenceScreen.getContext());
            appPreference.setIcon(applicationInfo.loadIcon(mPackageManager));
            appPreference.setSummary(mSelectedApp 
                                    + " target sdk "
                                    + applicationInfo.targetSdkVersion);
            appPreference.setOnPreferenceClickListener(preference -> {
                startAppPicker();
                return true;
            });
            preferenceScreen.addPreference(appPreference);
            PreferenceCategory defaultEnabled =
                new PreferenceCategory(preferenceScreen.getContext());
            defaultEnabled.setTitle(R.string.platform_compat_default_enabled_title);
            preferenceScreen.addPreference(defaultEnabled);
            Arrays.stream(mChanges)
                .filter(change -> change.defaultValue && (change.targetSdkThreshold == 0))
                .forEach(change -> defaultEnabled.addPreference(
                                                    createPreferenceForChange(
                                                        defaultEnabled.getContext(),
                                                        change,
                                                        configMappings)));
            PreferenceCategory defaultDisabled =
                new PreferenceCategory(preferenceScreen.getContext());
            defaultDisabled.setTitle(R.string.platform_compat_default_disabled_title);
            preferenceScreen.addPreference(defaultDisabled);
            Arrays.stream(mChanges)
                .filter(change -> !change.defaultValue && (change.targetSdkThreshold == 0))
                .forEach(change -> defaultDisabled.addPreference(createPreferenceForChange(
                                                                    defaultDisabled.getContext(),
                                                                    change,
                                                                    configMappings)));
            Set<Integer> sdkCategories = Arrays.stream(mChanges)
                                                .mapToInt(change -> change.targetSdkThreshold)
                                                .boxed()
                                                .collect(Collectors.toSet());
            sdkCategories.remove(new Integer(0));
            for (Integer sdk : sdkCategories) {
                PreferenceCategory category =
                    new PreferenceCategory(preferenceScreen.getContext());
                category.setTitle("Target SDK " + sdk);
                preferenceScreen.addPreference(category);
                Arrays.stream(mChanges)
                    .filter(change -> change.targetSdkThreshold == sdk)
                    .forEach(change -> category.addPreference(
                                                createPreferenceForChange(
                                                    preferenceScreen.getContext(),
                                                    change,
                                                    configMappings)));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
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
                ParcelableCompatibilityChangeConfig overrides =
                    new ParcelableCompatibilityChangeConfig(enabled, disabled);
                mPlatformCompat.setOverrides(overrides, mSelectedApp);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
