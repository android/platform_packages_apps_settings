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

package com.android.settings.wifi;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {
    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

    private final boolean mEdit;
    private final DialogInterface.OnClickListener mListener;
    private final AccessPoint mAccessPoint;

    private View mView;
    private WifiConfigController mController;

    public WifiDialog(Context context, DialogInterface.OnClickListener listener,
            AccessPoint accessPoint, boolean edit) {
        super(context, R.style.Theme_WifiDialog);
        mEdit = edit;
        mListener = listener;
        mAccessPoint = accessPoint;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    private void restoreAdvancedFields(Bundle savedInstanceState) {
        Integer proxySelection = (Integer) savedInstanceState.get("proxy_selection");
        if (proxySelection != null) {
            Spinner proxySettings = (Spinner) mView.findViewById(R.id.proxy_settings);
            if (proxySettings != null) proxySettings.setSelection(proxySelection);
        }
        Integer ipSelection = (Integer) savedInstanceState.get("ip_selection");
        if (ipSelection != null) {
            Spinner ipSettings = (Spinner) mView.findViewById(R.id.ip_settings);
            if (ipSettings != null) ipSettings.setSelection(ipSelection);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);
        if (savedInstanceState != null) {//Restore state only if it was saved before. Otherwise, the dialog is created for the first time
            Boolean show_advanced = (Boolean)savedInstanceState.get("show_advanced");
            if (show_advanced != null) {
                 View v=mView.findViewById(R.id.wifi_advanced_fields);
                 if (v != null) {
                     if (show_advanced) {
                         v.setVisibility(View.VISIBLE);
                         restoreAdvancedFields(savedInstanceState);
                     }
                     else v.setVisibility(View.GONE);
                 }
            }
            Boolean show_pass = (Boolean)savedInstanceState.get("show_pass");
            if (show_pass != null) {
                TextView mPasswordView = (TextView) mView.findViewById(R.id.password);
                if (mPasswordView != null) {
                    mPasswordView.setInputType(
                            InputType.TYPE_CLASS_TEXT | (show_pass ?
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD));
                }
            }
        }
        mController = new WifiConfigController(this, mView, mAccessPoint, mEdit);
        super.onCreate(savedInstanceState);
        /* During creation, the submit button can be unavailable to determine
         * visibility. Right after creation, update button visibility */
        mController.enableSubmitIfAppropriate();
    }

    @Override
    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getForgetButton() {
        return getButton(BUTTON_FORGET);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(BUTTON_SUBMIT, text, mListener);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(BUTTON_FORGET, text, mListener);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, mListener);
    }
    @Override
    public Bundle onSaveInstanceState() {
        Bundle b = super.onSaveInstanceState();
        CheckBox advanced = (CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox);
        CheckBox show_pass = (CheckBox) mView.findViewById(R.id.show_password);
        Spinner proxySettings = (Spinner) mView.findViewById(R.id.proxy_settings);
        Spinner ipSettings = (Spinner) mView.findViewById(R.id.ip_settings);
        if (advanced != null)
            b.putBoolean("show_advanced", advanced.isChecked());
        if (show_pass != null)
            b.putBoolean("show_pass", show_pass.isChecked());
        if (proxySettings != null)
            b.putInt("proxy_selection", proxySettings.getSelectedItemPosition());
        if (ipSettings != null)
            b.putInt("ip_selection", ipSettings.getSelectedItemPosition());
        return b;
    }
}
