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

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import android.util.Log;

public class WirelessDebuggingMasterSwitchController  extends AbstractPreferenceController
        implements PreferenceControllerMixin, SummaryUpdater.OnSummaryChangeListener,
        LifecycleObserver, OnResume, OnPause, OnStart, OnStop {
    final static String TAG = "AdbWirelessController";

    public static final String KEY_TOGGLE_ADB_WIRELESS = "toggle_adb_wireless";

    private MasterSwitchPreference mWirelessDebuggingPreference;
    private WirelessDebuggingEnabler mWirelessDebuggingEnabler;

    public WirelessDebuggingMasterSwitchController (Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWirelessDebuggingPreference = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_ADB_WIRELESS);
    }

    @Override
    public boolean isAvailable() {
        // TODO(joshuaduong): Wireless debugging is only available if the device is
        // connected a wireless network.
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_ADB_WIRELESS;
    }

    @Override
    public void onResume() {
        if (mWirelessDebuggingEnabler != null) {
            mWirelessDebuggingEnabler.resume(mContext);
        }
    }

    @Override
    public void onPause() {
        if (mWirelessDebuggingEnabler != null) {
            mWirelessDebuggingEnabler.pause();
        }
    }

    @Override
    public void onStart() {
        mWirelessDebuggingEnabler = new WirelessDebuggingEnabler(
                mContext,
                new MasterSwitchController(mWirelessDebuggingPreference));
    }

    @Override
    public void onStop() {
        if (mWirelessDebuggingEnabler != null) {
            mWirelessDebuggingEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onSummaryChanged(String summary) {
        // Maybe if we're paired with some device we can put something here?
    }
}
