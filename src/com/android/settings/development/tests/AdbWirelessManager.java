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

// Just a fake AdbWirelessManager for testing until the real one is actually
// up and running.
//

package com.android.settings.development.tests;

import android.content.Context;
import android.util.Log;

public class AdbWirelessManager {
    /**
     * Broadcast intent action indicating that ADB Wireless has been enabled,
     * disabled, enabling, disabling, or unknown. One extra provides this state
     * as an int. Another extra provides the previous state, if available.
     */
    public static final String ADB_WIRELESS_STATE_CHANGED_ACTION =
        "android.adb.wireless.ADB_WIRELESS_STATE_CHANGED";

    /**
     * The lookup key for an int that indicates whether ADB wireless is enabled,
     * disabled, enabling, disabling, or unknown. Retrieve it with
     * {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #ADB_WIRELESS_STATE_DISABLED
     * @see #ADB_WIRELESS_STATE_DISABLING
     * @see #ADB_WIRELESS_STATE_ENABLED
     * @see #ADB_WIRELESS_STATE_ENABLING
     * @see #ADB_WIRELESS_STATE_UNKNOWN
     */
    public static final String EXTRA_ADB_WIRELESS_STATE = "adb_wireless_state";
    /**
     * The previous ADB wireless state.
     *
     * @see #EXTRA_ADB_WIRELESS_STATE
     */
    public static final String EXTRA_PREVIOUS_ADB_WIRELESS_STATE =
        "previous_adb_wireless_state";

    /**
     * ADB wireless is currently being disabled. The state will change to
     * {@link #ADB_WIRELESS_STATE_DISABLED} if it finishes successfully.
     *
     * @see #ADB_WIRELESS_STATE_CHANGED_ACTION
     * @see #getAdbWirelessState()
     */
    public static final int ADB_WIRELESS_STATE_DISABLED = 0;

    /**
     * ADB wireless is disabled.
     *
     * @see #ADB_WIRELESS_STATE_CHANGED_ACTION
     * @see #getAdbWirelessState()
     */
    public static final int ADB_WIRELESS_STATE_DISABLING = 1;

    /**
     * ADB wireless is currently being enabled. The state will change to
     * {@link #ADB_WIRELESS_STATE_ENABLED} if it finishes successfully.
     *
     * @see #ADB_WIRELESS_STATE_CHANGED_ACTION
     * @see #getAdbWirelessState()
     */
    public static final int ADB_WIRELESS_STATE_ENABLING = 2;

    /**
     * ADB wireless is enabled.
     *
     * @see #ADB_WIRELESS_STATE_CHANGED_ACTION
     * @see #getAdbWirelessState()
     */
    public static final int ADB_WIRELESS_STATE_ENABLED = 3;

    /**
     * ADB wireless is in an unknown state. This state will occur when an error happens
     * while enabling or disabling.
     *
     * @see #ADB_WIRELESS_STATE_CHANGED_ACTION
     * @see #getAdbWirelessState()
     */
    public static final int ADB_WIRELESS_STATE_UNKNOWN = 4;

    /**
     * Gets the ADB wireless enabled state.
     * @return One of (@link #ADB_WIRELESS_STATE_DISABLED},
     *         {@link #ADB_WIRELESS_STATE_DISABLING}, {@link #ADB_WIRELESS_STATE_ENABLED},
     *         {@link #ADB_WIRELESS_STATE_ENABLING}, {@link #ADB_WIRELESS_STATE_UNKNOWN}
     */
    public static int getAdbWirelessState() {
        // TODO: remove the simulator
        if (mSimulator != null) {
            return mSimulator.getAdbWirelessState();
        }
        return ADB_WIRELESS_STATE_UNKNOWN;
    }

    /**
     * Enable/disable ADB wireless debugging. This will fail (return false) if the app
     * does not have the correct permissions.
     */
    public static boolean setAdbWirelessEnabled(boolean enabled) {
        // TODO: remove the simulator
        if (mSimulator != null) {
            if (enabled) {
                mSimulator.simulateFulfillEnableRequest();
            } else {
                mSimulator.simulateFulfillDisableRequest();
            }
        }
        return true;
    }

    static AdbWirelessStateSimulator mSimulator;
    public static void enableSimulationMode(Context context) {
        if (mSimulator == null) {
            mSimulator = new AdbWirelessStateSimulator(context);
        }
    }
    static final String TAG = "JoshAdbWirelessManager";
}
