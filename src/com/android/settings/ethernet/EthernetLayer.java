/*
 * Copyright (C) 2010 The Android-x86 Open Source Project
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

package com.android.settings.ethernet;

import static android.net.ethernet.EthernetManager.ETH_DEVICE_SCAN_RESULT_READY;

import com.android.settings.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.ethernet.EthernetManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EthernetLayer {
    private static final String TAG = "EthernetLayer";

    private EthernetManager mEthManager;
    private String[] mDevList;
    private EthernetConfigDialog mDialog;

    EthernetLayer (EthernetConfigDialog configdialog) {
        mDialog = configdialog;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(EthernetManager.ETH_DEVICE_SCAN_RESULT_READY)) {
                handleDevListChanges();
            }
        }
    };

    private void handleDevListChanges() {
        mDevList = mEthManager.getDeviceNameList();
        mDialog.updateDevNameList(mDevList);
    }
}
