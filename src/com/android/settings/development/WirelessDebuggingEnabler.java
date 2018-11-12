/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;

// TODO(joshuaduong): remove this once AdbWirelessManager is in.
import com.android.settings.development.tests.AdbWirelessManager;

public class WirelessDebuggingEnabler implements SwitchWidgetController.OnSwitchChangeListener  {
    String TAG = "AdbWirelessEnabler";

    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private boolean mListeningToOnSwitchChange = false;

    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION.equals(action)) {
                handleAdbWirelessStateChanged(AdbWirelessManager.getAdbWirelessState());
            }
        }
    };

    public WirelessDebuggingEnabler(Context context, SwitchWidgetController switchWidget) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);

        // TODO(joshuaduong): Remove this once adb wireless manager is in.
        AdbWirelessManager.enableSimulationMode(mContext);

        mIntentFilter = new IntentFilter(AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION);

        setupSwitchController();
    }

    public void setupSwitchController() {
        final int state = AdbWirelessManager.getAdbWirelessState();
        handleAdbWirelessStateChanged(state);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        mSwitchWidget.setupView();
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
        mContext.registerReceiver(mReceiver, mIntentFilter);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
    }

    private void handleAdbWirelessStateChanged(int state) {
        switch (state) {
            case AdbWirelessManager.ADB_WIRELESS_STATE_DISABLED:
                setSwitchBarChecked(false);
                mSwitchWidget.setEnabled(true);
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_DISABLING:
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_ENABLED:
                setSwitchBarChecked(true);
                mSwitchWidget.setEnabled(true);
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_ENABLING:
                break;
            default:
                setSwitchBarChecked(false);
                mSwitchWidget.setEnabled(true);
                break;
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mSwitchWidget.setChecked(checked);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (!AdbWirelessManager.setAdbWirelessEnabled(isChecked)) {
            // Error
            mSwitchWidget.setEnabled(true);
            Toast.makeText(mContext, R.string.adb_wireless_error, Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}

