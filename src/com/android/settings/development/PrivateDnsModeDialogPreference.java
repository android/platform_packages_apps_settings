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
import android.util.Log;
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


public class PrivateDnsModeDialogPreference extends CustomDialogPreference
        implements OnCheckedChangeListener, TextWatcher {
    private static final String TAG = PrivateDnsModeDialogPreference.class.getSimpleName();

    private static final String DEFAULT_MODE = PRIVATE_DNS_MODE_OPPORTUNISTIC;
    private static final String SETTINGS_KEY = Settings.Global.PRIVATE_DNS_MODE;
    private String mDialogValue;
    private EditText mEditText;

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
        final String settingsValue = getSettingsString();

        RadioButton rb = (RadioButton) view.findViewById(R.id.private_dns_mode_off);
        if (settingsValue.equals(PRIVATE_DNS_MODE_OFF)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        rb = (RadioButton) view.findViewById(R.id.private_dns_mode_opportunistic);
        if (settingsValue.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        rb = (RadioButton) view.findViewById(R.id.private_dns_mode_provider);
        if (settingsValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        mEditText = (EditText) view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.setOnEditorActionListener(
                (TextView tv, int actionId, KeyEvent k) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        saveDialogValue();
                        PrivateDnsModeDialogPreference.this.getDialog().dismiss();
                        return true;
                    }
                    return false;
                });
        mEditText.addTextChangedListener(this);

        // (Mostly) Fix the EditText field's indentation to align underneath the
        // displayed radio button text, and not under the radio button itself.
        final int padding = rb.isLayoutRtl()
                ? rb.getCompoundPaddingRight()
                : rb.getCompoundPaddingLeft();
        final MarginLayoutParams marginParams = (MarginLayoutParams) mEditText.getLayoutParams();
        marginParams.setMarginStart(marginParams.getMarginStart() + padding);
        mEditText.setLayoutParams(marginParams);

        if (settingsValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            mEditText.setEnabled(true);
            mEditText.setText(getHostname(settingsValue));
        } else {
            mEditText.setEnabled(false);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && !TextUtils.isEmpty(mDialogValue)) {
            saveDialogValue();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;

        switch (buttonView.getId()) {
            case R.id.private_dns_mode_off:
                setDialogValue(PRIVATE_DNS_MODE_OFF);
                break;
            case R.id.private_dns_mode_opportunistic:
                setDialogValue(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                break;
            case R.id.private_dns_mode_provider:
                setDialogValue(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                break;
            default:
                // Unknown button; ignored.
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

        // TODO: Display something like an "!" somewhere and clear it when the
        // hostname appears valid.
    }

    private void setDialogValue(String mode) {
        mDialogValue = mode;
        final boolean txtEnabled = mDialogValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        mEditText.setEnabled(txtEnabled);
    }

    private void saveDialogValue() {
        if (!isValidMode(mDialogValue)) {
            mDialogValue = DEFAULT_MODE;
        }

        if (mDialogValue.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            final String hostname = mEditText.getText().toString();
            if (isWeaklyValidatedHostname(hostname)) {
                mDialogValue = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME + hostname;
            } else {
                mDialogValue = PRIVATE_DNS_MODE_OPPORTUNISTIC;
            }
        }

        Log.w(TAG, String.format("Setting %s=%s", SETTINGS_KEY, mDialogValue));
        putSettingsString(mDialogValue);
    }

    private String getSettingsString() {
        final String value = Settings.Global.getString(
                getContext().getContentResolver(), SETTINGS_KEY);

        return isValidMode(value) ? value : DEFAULT_MODE;
    }

    private void putSettingsString(String value) {
        Settings.Global.putString(getContext().getContentResolver(), SETTINGS_KEY, value);
    }

    private static boolean isValidMode(String mode) {
        return !TextUtils.isEmpty(mode) && (
                mode.equals(PRIVATE_DNS_MODE_OFF) ||
                mode.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC) ||
                mode.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME));
    }

    private static boolean isWeaklyValidatedHostname(String hostname) {
        // TODO: Make stronger.
        final String WEAK_HOSTNAME_REGEX = "^[a-zA-Z0-9_.-]+$";
        return hostname.matches(WEAK_HOSTNAME_REGEX);
    }

    private static String getHostname(String setting) {
        if (TextUtils.isEmpty(setting)) return "";
        if (!setting.startsWith(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) return "";
        return setting.substring(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME.length());
    }
}
