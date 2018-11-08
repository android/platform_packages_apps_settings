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

import android.database.ContentObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;

public class WirelessDebuggingEnabler implements SwitchWidgetController.OnSwitchChangeListener {
    private final String TAG = this.getClass().getSimpleName();

    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private boolean mListeningToOnSwitchChange = false;
    private OnEnabledListener mListener;
    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsObserver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public WirelessDebuggingEnabler(Context context, SwitchWidgetController switchWidget,
            OnEnabledListener listener) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mListener = listener;

        mContentResolver = context.getContentResolver();
        setupSwitchController();
        mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                onWirelessDebuggingEnabled(isAdbWifiEnabled());
            }
        };
    }

    public void setupSwitchController() {
        onWirelessDebuggingEnabled(isAdbWifiEnabled());
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        mSwitchWidget.setupView();
    }

    private boolean isAdbWifiEnabled() {
        return Settings.Global.getInt(mContentResolver, Settings.Global.ADB_WIFI_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF)
                != AdbPreferenceController.ADB_SETTING_OFF;
    }

    public void teardownSwitchController() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        mContext = context;
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED), false,
                mSettingsObserver);
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    private void onWirelessDebuggingEnabled(boolean enabled) {
        mSwitchWidget.setChecked(enabled);
        mSwitchWidget.setEnabled(true);
        if (mListener != null) {
            mListener.onEnabled(enabled);
        }
    }

    protected void writeAdbWifiSetting(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_WIFI_ENABLED, enabled ? AdbPreferenceController.ADB_SETTING_ON
                : AdbPreferenceController.ADB_SETTING_OFF);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        writeAdbWifiSetting(isChecked);
        onWirelessDebuggingEnabled(isChecked);
        return true;
    }

    public interface OnEnabledListener {
        public void onEnabled(boolean enabled);
    }
}
