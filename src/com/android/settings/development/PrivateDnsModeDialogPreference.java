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

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference;

// XXX
import android.util.Log;

public class PrivateDnsModeDialogPreference extends CustomDialogPreference {
    private static final String TAG = "XXX";

    private static final String DEFAULT_MODE = PRIVATE_DNS_MODE_OPPORTUNISTIC;
    private String mSettingsValue;
    private String mDialogValue;

    private RadioButton mBtnOff;
    private RadioButton mBtnOpportunistic;
    private RadioButton mBtnProvider;
    private EditText mTxtHostname;

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
    }

    // This is called first when the dialog is launched.
    @Override
    protected void onBindDialogView(View view) {
        Log.w(TAG, "onBindDialogView");

        mBtnOff = (RadioButton) view.findViewById(R.id.private_dns_mode_off);
        mBtnOff.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {
                    if (isChecked) {
                        setDialogValue(PRIVATE_DNS_MODE_OFF);
                    }
                });
        mBtnOpportunistic = (RadioButton) view.findViewById(R.id.private_dns_mode_opportunistic);
        mBtnOpportunistic.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {
                    if (isChecked) {
                        setDialogValue(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                    }
                });
        mBtnProvider = (RadioButton) view.findViewById(R.id.private_dns_mode_provider);
        mBtnProvider.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {
                    if (isChecked) {
                        setDialogValue(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    }
                });
        mTxtHostname = (EditText) view.findViewById(R.id.private_dns_mode_provider_hostname);

        loadSettingsValue();

        if (mSettingsValue.equals(PRIVATE_DNS_MODE_OFF)) {
            mBtnOff.setChecked(true);
        } else if (mSettingsValue.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC)) {
            mBtnOpportunistic.setChecked(true);
        } else if (mSettingsValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            mBtnProvider.setChecked(true);
            mTxtHostname.setText(
                    mSettingsValue.substring(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME.length()));
        }
    }

    // This is called after onBindDialogView().
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        Log.w(TAG, "onPrepareDialogBuilder");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) return;

        Log.w(TAG, "onDialogClosed: " + positiveResult);
        saveDialogValue();
    }

    private void loadSettingsValue() {
        mSettingsValue = Settings.Global.getString(
                getContext().getContentResolver(),
                Settings.Global.PRIVATE_DNS_MODE);

        if (!isValidMode(mSettingsValue)) {
            mSettingsValue = DEFAULT_MODE;
        }

        Log.w(TAG, "mSettingsValue=" + mSettingsValue);
    }

    private void setDialogValue(String mode) {
        mDialogValue = mode;
        final boolean txtEnabled = mDialogValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        mTxtHostname.setEnabled(txtEnabled);
    }

    private void saveDialogValue() {
        if (!isValidMode(mDialogValue)) {
            mDialogValue = DEFAULT_MODE;
        }

        if (mDialogValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            mDialogValue = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME + mTxtHostname.getText().toString();
        }

        Log.w(TAG, "mDialogValue=" + mDialogValue);
        Settings.Global.putString(
                getContext().getContentResolver(),
                Settings.Global.PRIVATE_DNS_MODE,
                mDialogValue);
    }

    private static boolean isValidMode(String mode) {
        return !TextUtils.isEmpty(mode) && (
                mode.equals(PRIVATE_DNS_MODE_OFF) ||
                mode.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC) ||
                mode.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME));
    }
}
