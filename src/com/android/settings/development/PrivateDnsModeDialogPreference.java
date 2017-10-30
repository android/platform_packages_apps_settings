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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference;

// XXX
import android.util.Log;

public class PrivateDnsModeDialogPreference extends CustomDialogPreference
        implements OnCheckedChangeListener, TextWatcher {
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
        Log.w(TAG, "onBindDialogView: " + 5);

        loadSettingsValue();

        mBtnOff = (RadioButton) view.findViewById(R.id.private_dns_mode_off);
        mBtnOff.setOnCheckedChangeListener(this);
        mBtnOpportunistic = (RadioButton) view.findViewById(R.id.private_dns_mode_opportunistic);
        mBtnOpportunistic.setOnCheckedChangeListener(this);
        mBtnProvider = (RadioButton) view.findViewById(R.id.private_dns_mode_provider);
        mBtnProvider.setOnCheckedChangeListener(this);
        mTxtHostname = (EditText) view.findViewById(R.id.private_dns_mode_provider_hostname);
        mTxtHostname.setOnEditorActionListener(
                (TextView tv, int actionId, KeyEvent k) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        saveDialogValue();
                        PrivateDnsModeDialogPreference.this.getDialog().dismiss();
                        return true;
                    }
                    return false;
                });
        mTxtHostname.addTextChangedListener(this);
        final int padding = mBtnProvider.isLayoutRtl()
                ? mBtnProvider.getCompoundPaddingRight()
                : mBtnProvider.getCompoundPaddingLeft();
        final MarginLayoutParams marginParams = (MarginLayoutParams) mTxtHostname.getLayoutParams();
        Log.w(TAG, "pre layoutParams=" + marginParams);
        marginParams.setMarginStart(marginParams.getMarginStart() + padding);
        Log.w(TAG, "post layoutParams=" + marginParams);
        mTxtHostname.setLayoutParams(marginParams);

        if (mSettingsValue.equals(PRIVATE_DNS_MODE_OFF)) {
            mBtnOff.setChecked(true);
        } else if (mSettingsValue.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC)) {
            mBtnOpportunistic.setChecked(true);
        } else if (mSettingsValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            mBtnProvider.setChecked(true);
        }

        if (mSettingsValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            mTxtHostname.setEnabled(true);
            mTxtHostname.setText(getHostname(mSettingsValue));
        } else {
            mTxtHostname.setEnabled(false);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) saveDialogValue();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;

        switch (buttonView.getId()) {
            case R.id.private_dns_mode_off:
                setDialogValue(PRIVATE_DNS_MODE_OFF);
            case R.id.private_dns_mode_opportunistic:
                setDialogValue(PRIVATE_DNS_MODE_OPPORTUNISTIC);
            case R.id.private_dns_mode_provider:
                setDialogValue(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                break;
            default:
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { return; }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { return; }

    @Override
    public void afterTextChanged(Editable s) {
        final String hostname = s.toString();
        if (isWeaklyValidatedHostname(hostname)) return;

        // Display a "!" somewhere.
        Log.w(TAG, "invalid looking hostname: " + hostname);
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
            final String hostname = mTxtHostname.getText().toString();
            if (isWeaklyValidatedHostname(hostname)) {
                mDialogValue = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME + hostname;
            } else {
                mDialogValue = PRIVATE_DNS_MODE_OPPORTUNISTIC;
            }
        }

        Log.w(TAG, "saving mDialogValue=" + mDialogValue);
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

    private static boolean isWeaklyValidatedHostname(String hostname) {
        final String WEAK_HOSTNAME_REGEX = "^[a-zA-Z0-9_.-]+$";
        return hostname.matches(WEAK_HOSTNAME_REGEX);
    }

    private static String getHostname(String setting) {
        if (!TextUtils.isEmpty(setting) &&
                setting.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            return setting.substring(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME.length());
        }

        return "";
    }
}
