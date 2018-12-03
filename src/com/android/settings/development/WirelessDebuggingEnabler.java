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
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;

public class WirelessDebuggingEnabler implements SwitchWidgetController.OnSwitchChangeListener  {
    private final String TAG = this.getClass().getSimpleName();

    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private Context mAppContext;
    private boolean mListeningToOnSwitchChange = false;
    private OnEnabledListener mListener;
    private final IAdbManager mAdbManager;

    public WirelessDebuggingEnabler(Context context, SwitchWidgetController switchWidget, OnEnabledListener listener) {
        mContext = context;
        mAppContext = mContext.getApplicationContext();
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mListener = listener;
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));

        setupSwitchController();
    }

    public void setupSwitchController() {
        try {
            final boolean enabled = mAdbManager.isEnabled();
            onWirelessDebuggingEnabled(enabled);
            if (!mListeningToOnSwitchChange) {
                mSwitchWidget.startListening();
                mListeningToOnSwitchChange = true;
            }
            mSwitchWidget.setupView();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to check if ADB wireless is enabled");
        }
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
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
    }

    private void onWirelessDebuggingEnabled(boolean enabled) {
        setSwitchBarChecked(enabled);
        mSwitchWidget.setEnabled(true);
        if (mListener != null) {
            mListener.onEnabled(enabled);
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mSwitchWidget.setChecked(checked);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        try {
            mAdbManager.enableAdbWireless(isChecked);
            onWirelessDebuggingEnabled(isChecked);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to enable ADB wireless");
        } finally {
            return false;
        }
    }

    public interface OnEnabledListener {
        public void onEnabled(boolean enabled);
    }
}

