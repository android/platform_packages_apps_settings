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

import android.app.Instrumentation;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.test.ActivityInstrumentationTestCase2;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.widget.EditText;
import android.widget.TextView;

public class FWifiApDialogTooLongTests extends ActivityInstrumentationTestCase2<WifiApSettings> {

    public FWifiApDialogTooLongTests() {
        super("com.android.settings", WifiApSettings.class);
    }

    private Instrumentation mInstrumentation;
    private WifiApSettings mActivity;
    private WifiApDialog mWifiApDialog;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = (WifiApSettings) getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        if (mWifiApDialog != null) {
            mWifiApDialog.dismiss();
        }
        super.tearDown();
    }

    /**
     * Tests that the WPA password filter is limiting the length.
     */
    public void testUiTooLongWPAPassword() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
        mWifiApDialog = new WifiApDialog(mActivity, mActivity, wifiConfig);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mWifiApDialog.show();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Password input
        final String pwdHex64 = "123456789012345678901234567890123456789012345678901234567890abcd";
        final String pwdHex65 = "123456789012345678901234567890123456789012345678901234567890abcde";
        final String pwdAscii63 = "123456789012345678901234567890123456789012345678901234567890xyz";
        final String pwdAscii64 =
                "123456789012345678901234567890123456789012345678901234567890xyzz";
        final EditText pwdTextView = (EditText)mWifiApDialog.findViewById(R.id.password);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                pwdTextView.setText(pwdHex64); // Acceptable
                pwdTextView.setText(pwdHex65); // Should be denied
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("Password is not 64 hex tokens", 64, pwdTextView.getText().length());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                pwdTextView.setText(pwdAscii63); // Acceptable
                pwdTextView.setText(pwdAscii64); // Should be denied
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("Password is not 63 ascii tokens", 63, pwdTextView.getText().length());
    }
}
