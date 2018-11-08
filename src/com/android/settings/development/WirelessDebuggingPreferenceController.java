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
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import android.util.Log;

public class WirelessDebuggingPreferenceController  extends AbstractPreferenceController
        implements PreferenceControllerMixin, SummaryUpdater.OnSummaryChangeListener,
        LifecycleObserver, OnResume, OnPause, OnStart, OnStop {
    private final String TAG = this.getClass().getSimpleName();

    public static final String KEY_TOGGLE_ADB_WIRELESS = "toggle_adb_wireless";

    private MasterSwitchPreference mWirelessDebuggingPreference;
    private WirelessDebuggingEnabler mWirelessDebuggingEnabler;

    public WirelessDebuggingPreferenceController (Context context, Lifecycle lifecycle) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWirelessDebuggingPreference = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_ADB_WIRELESS);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_ADB_WIRELESS;
    }

    @Override
    public void onStart() {
        // Must define (abstract)
    }

    @Override
    public void onResume() {
        mWirelessDebuggingEnabler = new WirelessDebuggingEnabler(
                mContext,
                new MasterSwitchController(mWirelessDebuggingPreference), null);
        mWirelessDebuggingEnabler.resume(mContext);
    }

    @Override
    public void onPause() {
        mWirelessDebuggingEnabler.pause();
    }

    @Override
    public void onStop() {
        mWirelessDebuggingEnabler.teardownSwitchController();
        mWirelessDebuggingEnabler = null;
    }

    @Override
    public void onSummaryChanged(String summary) {
        // We don't have any summary
    }
}
