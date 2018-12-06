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
import android.debug.IAdbManager;
import android.os.IBinder;
import android.util.Log;

public class WirelessDebuggingManager implements IAdbManager {
    private final String TAG = this.getClass().getSimpleName();

    public IBinder asBinder() { return null; }
    public void allowDebugging(boolean alwaysAllow, String pubkey) {}
    public void denyDebugging() {}
    public void clearDebuggingKeys() {}

    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Enable/disable ADB wireless debugging. This will fail (return false) if the app
     * does not have the correct permissions. When passing enabled = true, WirelessDebuggingManager
     * will start to get any updates for the paired devices list. Any updates will be notified via
     * @see WIRELESS_DEBUG_PAIRED_DEVICES_ACTION.
     */
    public void enableAdbWireless(boolean enabled) {
        mEnabled = enabled;
        if (enabled) {
            mPairedDeviceGenerator.start();
        } else {
            mPairedDeviceGenerator.stop();
        }
    }

    public void pairDevice(int id, String qrcode) {
        mPairedDeviceGenerator.pairDevice(id, qrcode);
    }

    public void unPairDevice(int id) {
        mPairedDeviceGenerator.unPairDevice(id);
    }

    public void queryAdbWirelessPairedDevices() {
        mPairedDeviceGenerator.queryAdbWirelessPairedDevices();
    }

    public void queryAdbWirelessPairingDevices() {
        mPairedDeviceGenerator.queryAdbWirelessPairingDevices();
    }

    public void cancelPairing(int id) {
        mPairedDeviceGenerator.cancelPairing(id);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
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
    public static WirelessDebuggingManager getInstance(Context context) {
        if (sWirelessDebuggingManager == null) {
            sWirelessDebuggingManager = new WirelessDebuggingManager(context);
        }
        return sWirelessDebuggingManager;
    }
}
