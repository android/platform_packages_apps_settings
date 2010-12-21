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
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

public class WifiDialogTests extends ActivityInstrumentationTestCase2<WifiSettings> {

    public WifiDialogTests() {
        super("com.android.settings.wifi", WifiSettings.class);
    }

    private Instrumentation mInstrumentation;
    private WifiSettings mActivity;
    private WifiDialog mWifiDialog;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = (WifiSettings) getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        if (mWifiDialog != null) {
            mWifiDialog.dismiss();
        }
        super.tearDown();
    }

    /**
     * Tests that it is only possible to enter 32 tokens into SSID
     * (length according to the standard).
     */
    public void testUiTooLongSsid() {
        mWifiDialog = new WifiDialog(mActivity, mActivity, null, true);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
               mWifiDialog.show();
            }
        });
        mInstrumentation.waitForIdleSync();

        // SSID input
        final String ssid32 ="12345678901234567890123456789012";  // 32 tokens
        final String ssid33 ="123456789012345678901234567890123";  // 33 tokens
        final TextView ssidTextView = (TextView) mWifiDialog.findViewById(R.id.ssid);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                ssidTextView.setText(ssid32); // Acceptable
                ssidTextView.setText(ssid33); // Should be denied
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("SSID is not 32 tokens", 32, ssidTextView.getText().length());
    }

}
