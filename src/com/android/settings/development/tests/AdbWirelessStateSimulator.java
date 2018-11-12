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
import android.content.Intent;
import android.util.Log;

// Class to simulate transitions between adb wireless states.
// Pass this to the fake AdbWirelessManager to start simulating.
public class AdbWirelessStateSimulator {
    final String TAG = "AdbWirelessStateSimulator";

    int mAdbWirelessState = AdbWirelessManager.ADB_WIRELESS_STATE_DISABLED;
    Context mContext;

    public AdbWirelessStateSimulator(Context context) {
        mContext = context;
    }

    public int getAdbWirelessState() {
        return mAdbWirelessState;
    }

    public void simulateFulfillEnableRequest() {
        Thread t = new Thread(() -> {
            AdbWirelessStateSenderThread a1 = new AdbWirelessStateSenderThread(
                    AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION, AdbWirelessManager.ADB_WIRELESS_STATE_ENABLING,
                    mAdbWirelessState,
                    mContext);
            mAdbWirelessState = AdbWirelessManager.ADB_WIRELESS_STATE_ENABLING;
            a1.start();
            try {
                a1.join();
            } catch (Exception e) {
                return;
            }

            a1 = new AdbWirelessStateSenderThread(
                    AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION,
                    AdbWirelessManager.ADB_WIRELESS_STATE_ENABLED,
                    mAdbWirelessState,
                    mContext);
            mAdbWirelessState = AdbWirelessManager.ADB_WIRELESS_STATE_ENABLED;
            a1.start();
        });
        t.start();
    }

    public void simulateFulfillDisableRequest() {
        Thread t = new Thread(() -> {
            AdbWirelessStateSenderThread a1 = new AdbWirelessStateSenderThread(
                    AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION,
                    AdbWirelessManager.ADB_WIRELESS_STATE_DISABLING,
                    mAdbWirelessState,
                    mContext);
            mAdbWirelessState = AdbWirelessManager.ADB_WIRELESS_STATE_DISABLING;
            a1.start();
            try {
                a1.join();
            } catch (Exception e) {
                return;
            }

            a1 = new AdbWirelessStateSenderThread(
                    AdbWirelessManager.ADB_WIRELESS_STATE_CHANGED_ACTION,
                    AdbWirelessManager.ADB_WIRELESS_STATE_DISABLED,
                    mAdbWirelessState,
                    mContext);
            mAdbWirelessState = AdbWirelessManager.ADB_WIRELESS_STATE_DISABLED;
            a1.start();
        });
        t.start();
    }
}

// Just a simple class to send an intent from a different thread so we don't
// block the UI. For testing purposes only.
class AdbWirelessStateSenderThread extends Thread {
    String mAction;
    int mState;
    int mPreviousState;
    Context mContext;

    final String TAG = "AdbWirelessStateSenderThread";

    public AdbWirelessStateSenderThread(String action, int state, int previousState, Context context) {
        mAction = action;
        mState = state;
        mPreviousState = previousState;
        mContext = context;
    }

    public void run() {
        Intent intent = new Intent();
        intent.setAction(mAction);
        intent.putExtra(AdbWirelessManager.EXTRA_ADB_WIRELESS_STATE, mState);
        intent.putExtra(AdbWirelessManager.EXTRA_PREVIOUS_ADB_WIRELESS_STATE, mPreviousState);
        mContext.sendBroadcast(intent);
    }
}
