/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.content.res.Resources;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

// test code
import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.Constants;

/**
 * The class for allowing UIs like {@link WifiDialog} and {@link WifiConfigUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 */
public class AdbWirelessDialogController {
    private static final String TAG = "AdbWirelessDialogController";

    private final AdbWirelessDialogUiBase mUi;
    private final View mView;

    private int mMode;

    // The dialog for showing the six-digit code
    private TextView mPairingCodeTitle;
    private TextView mSixDigitCode;

    // The dialog for showing pairing failed message
    private TextView mFailedMsg;

    private Context mContext;

    public AdbWirelessDialogController(AdbWirelessDialogUiBase parent, View view,
            int mode) {
        mUi = parent;
        mView = view;
        mMode = mode;

        mContext = mUi.getContext();
        final Resources res = mContext.getResources();

        mSixDigitCode = mView.findViewById(R.id.pairing_code);

        switch (mMode) {
            case AdbWirelessDialogUiBase.MODE_PAIRING:
                String title = res.getString(R.string.adb_pairing_device_dialog_title);
                mUi.setTitle(title);
                mView.findViewById(R.id.l_pairing_six_digit).setVisibility(View.VISIBLE);
                mUi.setCancelButton(res.getString(R.string.cancel));
                mUi.setCanceledOnTouchOutside(false);
                break;
            case AdbWirelessDialogUiBase.MODE_PAIRING_FAILED:
                String msg = res.getString(R.string.adb_pairing_device_dialog_failed_msg);
                mUi.setTitle(R.string.adb_pairing_device_dialog_failed_title);
                mView.findViewById(R.id.l_pairing_failed).setVisibility(View.VISIBLE);
                mFailedMsg = (TextView) mView.findViewById(R.id.pairing_failed_label);
                mFailedMsg.setText(msg);
                mUi.setSubmitButton(res.getString(R.string.okay));
                break;
            case AdbWirelessDialogUiBase.MODE_QRCODE_FAILED:
                mUi.setTitle(R.string.adb_pairing_device_dialog_failed_title);
                mView.findViewById(R.id.l_qrcode_pairing_failed).setVisibility(View.VISIBLE);
                mUi.setSubmitButton(res.getString(R.string.okay));
                break;
            case AdbWirelessDialogUiBase.MODE_DISCOVERY_FAILED:
                mUi.setTitle(R.string.adb_discovery_enable_failed_title);
                mView.findViewById(R.id.l_discovery_failed).setVisibility(View.VISIBLE);
                mUi.setSubmitButton(res.getString(R.string.okay));
                break;
        }

        // After done view show and hide, request focus from parent view
        mView.findViewById(R.id.l_adbwirelessdialog).requestFocus();
    }

    public void setPairingCode(String code) {
        mSixDigitCode.setText(code);
    }
}
