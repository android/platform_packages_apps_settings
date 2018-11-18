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

    // remove once we get updated wirelessdebuggingmanager
    public static class PairedDevice implements Serializable {
        Integer mDeviceId;
        String mMacAddress;
        String mDeviceName;
        boolean mConnected;

        public PairedDevice(Integer id, String macAddr, String deviceName, boolean connected) {
            mDeviceId = id;
            mMacAddress = macAddr;
            mDeviceName = deviceName;
            mConnected = connected;
        }

        public Integer getDeviceId() { return mDeviceId; }
        public String getMacAddress() { return mMacAddress; }
        public String getDeviceName() { return mDeviceName; }
        public boolean isConnected() { return mConnected; }
    }

    /**
     * Broadcast intent action indicating a new list of paired devices is available.
     * Get the data by calling (HashMap<Integer,PairedDevice>)
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
    public static final String AUTH_CODE_EXTRA = "auth_code";

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
            //TODO: turn on the paired device generator
            mPairedDeviceGenerator.start();
        } else {
            mPairedDeviceGenerator.stop();
        }
    }

    public void pair(Integer id, String qrcode) {
      // not implemented
    }

    public void unpair(Integer id) {
      // not implemented
    }

    public void requestPairedList() {
        mPairedDeviceGenerator.requestPairedList();
    }

    public void requestPairingList() {
        mPairedDeviceGenerator.requestPairingList();
    }

    protected WirelessDebuggingManager(Context appContext) {
        mAppContext = appContext;
        mPairedDeviceGenerator = new PairedDeviceGenerator(mAppContext);
    }

    //////// Test code /////////////
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
