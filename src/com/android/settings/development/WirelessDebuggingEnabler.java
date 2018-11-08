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

import android.content.Context;
import android.util.Log;

import com.android.settings.widget.SwitchWidgetController;

public class WirelessDebuggingEnabler implements SwitchWidgetController.OnSwitchChangeListener  {
    private final String TAG = this.getClass().getSimpleName();

    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private boolean mListeningToOnSwitchChange = false;
    private OnEnabledListener mListener;

    public WirelessDebuggingEnabler(Context context, SwitchWidgetController switchWidget, OnEnabledListener listener) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mListener = listener;

        setupSwitchController();
    }

    public void setupSwitchController() {
        // TODO: Get Adb wireless enabled state
        onWirelessDebuggingEnabled(false);
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
        onWirelessDebuggingEnabled(isChecked);
        return true;
    }

    public interface OnEnabledListener {
        public void onEnabled(boolean enabled);
    }
}

