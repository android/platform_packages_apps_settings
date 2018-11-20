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
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.development.tests.WirelessDebuggingManager.PairedDevice;

import java.lang.Runnable;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Class to provide any fake data on a different thread to WirelessDebuggingManager.
// It basically functions as a fake adbd.
public class PairedDeviceGenerator implements Runnable {
    private final String TAG = this.getClass().getSimpleName();

    private Context mAppContext;
    private ScheduledExecutorService mScheduledTaskExecutor;
    private boolean mStarted = false;

    private static final HashMap<Integer, PairedDevice> mTestDevices = createPairedDevices();

    private static HashMap<Integer, PairedDevice> createPairedDevices() {
        HashMap<Integer, PairedDevice> map = new HashMap<Integer, PairedDevice>();
        map.put(0, new PairedDevice(0, "32:07:43:05:2B:05", "Josh's Macbook", false));
        map.put(1, new PairedDevice(1, "89:D2:77:50:76:E7", "John's Chromebook", false));
        map.put(2, new PairedDevice(2, "04:6C:19:9C:E0:F1", "Bob's Windows", false));
        map.put(3, new PairedDevice(3, "0E:3F:78:53:00:96", "Mary's mainframe", false));
        map.put(4, new PairedDevice(4, "F5:C8:96:50:8D:99", "Sam's Ubuntu", false));
        return map;
    }

    private HashMap<Integer, PairedDevice> mDeviceMap;

    public PairedDeviceGenerator(Context appContext) {
        mAppContext = appContext;
        mScheduledTaskExecutor = Executors.newScheduledThreadPool(1);
        mDeviceMap = new HashMap<Integer, PairedDevice>();
    }

    // force grab the device list. Will send the action event.
    public void requestPairedList() {
        Intent intent = new Intent(WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION);
        intent.putExtra(WirelessDebuggingManager.DEVICE_LIST_EXTRA, mDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void requestPairingList() {
      // not implemented
    }

    public void pair(Integer id, String qrcode) {
      // not implemented
    }

    public void unpair(Integer id) {
      // not implemented
    }

    public void start() {
        if (mStarted) {
            return;
        }

        Log.i(TAG, "Starting PairedDeviceGenerator");
        // This schedules a task to run every 5 seconds
        mScheduledTaskExecutor.scheduleAtFixedRate(this, 0, 10, TimeUnit.SECONDS);
        mStarted = true;
    }

    public void stop() {
        if (!mStarted) {
            return;
        }

        Log.i(TAG, "Stopping PairedDeviceGenerator");
        mScheduledTaskExecutor.shutdownNow();
        try {
            mScheduledTaskExecutor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Unable to stop " + TAG);
        }
        mStarted = false;
    }

    // Filled mDeviceMap with a random subset of mTestDevices
    private void randomizeDeviceMap() {
        mDeviceMap.clear();

        Random rand = new Random();
        // Get a random subset of mTestDevices
        int numDevices = rand.nextInt(mTestDevices.size() + 1);

        if (numDevices == 0) {
            return;
        }

        // Grab randomly numDevices from mTestDevices

        // Generates a shuffled list of numbers from 0..numDevices
        List<Integer> integers = IntStream.range(0, mTestDevices.size())
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(integers);

        for (int i = 0; i < numDevices; ++i) {
            mDeviceMap.put(integers.get(i), mTestDevices.get(integers.get(i)));
            // Randomly choose whether the device is connected or not
            mDeviceMap.get(integers.get(i)).setConnected(
                rand.nextBoolean());
        }
    }

    public void run() {
        try {
        randomizeDeviceMap();
        Intent intent = new Intent(WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION);
        intent.putExtra(WirelessDebuggingManager.DEVICE_LIST_EXTRA, mDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Exception e) {
            Log.w(TAG, "Something failed running the paired generator");
            e.printStackTrace();
        }

        // TODO: send pairing list as well
    }
}

