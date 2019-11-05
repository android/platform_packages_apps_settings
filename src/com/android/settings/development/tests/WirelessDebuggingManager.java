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

// Just a fake WirelessDebuggingManager for testing until the real one is actually
// up and running.
//

package com.android.settings.development.tests;

import android.content.Context;
import android.util.Log;

import java.io.Serializable;
import java.util.HashMap;

public class WirelessDebuggingManager {
    private final String TAG = this.getClass().getSimpleName();

    /**
     * Broadcast intent action indicating a new list of paired devices is available.
     * Get the data by calling (HashMap<String, PairDevice>)
     * intent.getSerializableExtra(WirelessDebuggingManager.PAIRED_DEVICES_EXTRA)
     */
    public static final String WIRELESS_DEBUG_PAIRED_LIST_ACTION =
            "com.android.settings.development.tests.action.WIRELESS_DEBUG_PAIRED_LIST";
    public static final String WIRELESS_DEBUG_PAIRING_LIST_ACTION =
            "com.android.settings.development.tests.action.WIRELESS_DEBUG_PAIRING_LIST";
    public static final String WIRELESS_DEBUG_PAIR_STATUS_ACTION =
            "com.android.settings.development.tests.action.WIRELESS_DEBUG_PAIR_STATUS_ACTION";
    public static final String WIRELESS_DEBUG_UNPAIR_STATUS_ACTION =
            "com.android.settings.development.tests.action.WIRELESS_DEBUG_UNPAIR_STATUS_ACTION";

    /**
     * The extra key to get the paired devices list.
     * @see #WIRELESS_DEBUG_PAIRED_DEVICES_ACTION
     */
    public static final String DEVICE_LIST_EXTRA = "map";
    public static final String PAIR_STATUS_EXTRA = "pair_status_type";
    public static final String PAIR_ID_EXTRA = "pair_id";
    public static final String PAIRED_DEVICE_EXTRA = "paired_device";
    public static final String AUTH_CODE_EXTRA = "auth_code";
    public static final String RESULT_CODE_EXTRA = "result_code";

    /**
     * Status codes for connecting/disconnecting, pairing/unpairing
     * @see #RESULT_CODE_EXTRA
     */
    public static final int RESULT_OK = 0;
    public static final int RESULT_FAILED = 1;

    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Enable/disable ADB wireless debugging. This will fail (return false) if the app
     * does not have the correct permissions. When passing enabled = true, WirelessDebuggingManager
     * will start to get any updates for the paired devices list. Any updates will be notified via
     * @see WIRELESS_DEBUG_PAIRED_DEVICES_ACTION.
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (enabled) {
            mPairedDeviceGenerator.start();
        } else {
            mPairedDeviceGenerator.stop();
        }
    }

    public void pairDevice(int mode, String guid, String qrcode) {
        mPairedDeviceGenerator.pairDevice(mode, guid, qrcode);
    }

    public void unpairDevice(String guid) {
        mPairedDeviceGenerator.unpairDevice(guid);
    }

    public void requestPairedList() {
        mPairedDeviceGenerator.requestPairedList();
    }

    public void requestPairingList() {
        mPairedDeviceGenerator.requestPairingList();
    }

    public void cancelPairing(int mode, String guid) {
        mPairedDeviceGenerator.cancelPairing(mode, guid);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setDiscoverable(int mode, boolean enable) {
        mPairedDeviceGenerator.setDiscoverable(mode, enable);
    }

    protected WirelessDebuggingManager(Context context) {
        mAppContext = context.getApplicationContext();
        mPairedDeviceGenerator = new PairedDeviceGenerator(mAppContext);
    }

    //////// Test code /////////////
    private String mName = "Josh's Pixel 2";
    private boolean mEnabled = false;
    private Context mAppContext;
    private PairedDeviceGenerator mPairedDeviceGenerator;
    static WirelessDebuggingManager sWirelessDebuggingManager;
    // This will later be equivalent to getSystemService(ADB_WIRELESS)
    public static WirelessDebuggingManager getInstance(Context appContext) {
        if (sWirelessDebuggingManager == null) {
            sWirelessDebuggingManager = new WirelessDebuggingManager(appContext);
        }
        return sWirelessDebuggingManager;
    }
}
