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
import android.debug.AdbManager;
import android.debug.PairDevice;
import android.os.UserHandle;
import android.util.Log;

import java.lang.InterruptedException;
import java.lang.Runnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
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

    // the initial list of devices currently paired with.
    private static HashMap<Integer, PairDevice> mPairedDevices = createPairedDevices();

    // the initial list of devices ready to be paired with.
    private static HashMap<Integer, PairDevice> mPairingDevices = createPairingDevices();

    private static HashMap<Integer, PairDevice> createPairedDevices() {
        HashMap<Integer, PairDevice> map = new HashMap<Integer, PairDevice>();
        map.put(0, new PairDevice(0, "32:07:43:05:2B:05", "Josh's Macbook", false));
        map.put(1, new PairDevice(1, "89:D2:77:50:76:E7", "John's Chromebook", false));
        map.put(2, new PairDevice(2, "04:6C:19:9C:E0:F1", "Bob's Windows", false));
        map.put(3, new PairDevice(3, "0E:3F:78:53:00:96", "Mary's mainframe", false));
        map.put(4, new PairDevice(4, "F5:C8:96:50:8D:99", "Sam's Ubuntu", false));
        return map;
    }

    private static HashMap<Integer, PairDevice> createPairingDevices() {
        HashMap<Integer, PairDevice> map = new HashMap<Integer, PairDevice>();
        map.put(5, new PairDevice(5, "32:07:55:05:2B:05", "Jane's Macbook Pro", false));
        map.put(6, new PairDevice(6, "89:D2:89:50:76:E7", "Jim's iMac", false));
        map.put(7, new PairDevice(7, "04:6C:6A:9C:E0:F1", "Suzie's Windows", false));
        map.put(8, new PairDevice(8, "0E:3F:12:53:00:96", "Al's Ubuntu", false));
        map.put(9, new PairDevice(9, "F5:C8:9F:50:8D:99", "Jill's unknown", false));
        return map;
    }

    // Pairing simulation threads that are currently running.
    private HashMap<Integer, ScheduledFuture> mPairingThreads;

    private HashMap<Integer, PairDevice> mPairedDeviceMap;
    private HashMap<Integer, PairDevice> mPairingDeviceMap;

    public PairedDeviceGenerator(Context appContext) {
        mAppContext = appContext;
        mScheduledTaskExecutor = Executors.newScheduledThreadPool(3);
        mPairingThreads = new HashMap<Integer, ScheduledFuture>();
    }

    // force grab the paired device list. Will send the action event.
    public void queryAdbWirelessPairedDevices() {
        Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION);
        intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, mPairedDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    // force grab the pairing device list. Will send the action event.
    public void queryAdbWirelessPairingDevices() {
        Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
        intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, mPairingDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void pairDevice(Integer id, String qrcode) {
        if (qrcode == null || qrcode.isEmpty()) {
            ScheduledFuture future = mScheduledTaskExecutor.schedule(
                    new DevicePairingThread(mAppContext,
                        mPairingDevices.get(id)),
                    1,
                    TimeUnit.SECONDS);
            mPairingThreads.put(id, future);
        }
    }

    public void unPairDevice(Integer id) {
        // Move it into the pairing devices hash map.
        mPairingDevices.put(id, mPairedDevices.get(id));
        mPairedDevices.remove(id);
    }

    public void cancelPairing(Integer id) {
        ScheduledFuture future = mPairingThreads.get(id);
        if (future != null) {
            future.cancel(true);
        }
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

    // Generates and returns a random subset of |map|. Set |randomizeConnected| to true to
    // also randomize whether the device is connected or not. Otherwise, all the devices will
    // be set to false.
    private HashMap<Integer, PairDevice> randomizeDeviceMap(HashMap<Integer, PairDevice> map, boolean randomizeConnected) {
        HashMap<Integer, PairDevice> newMap = new HashMap<Integer, PairDevice>();

        Log.i(TAG, "randomizeDeviceMap() device list: " + map);
        Random rand = new Random();
        // Get a random subset of map
        int numDevices = rand.nextInt(map.size() + 1);

        if (numDevices == 0) {
            return newMap;
        }

        // Grab randomly numDevices from map
        List<Integer> deviceIds = new ArrayList<Integer>(map.keySet());
        Collections.shuffle(deviceIds);

        for (int i = 0; i < numDevices; ++i) {
            newMap.put(deviceIds.get(i), map.get(deviceIds.get(i)));
            if (randomizeConnected) {
            // Randomly choose whether the device is connected or not
                newMap.get(deviceIds.get(i)).setConnected(
                    rand.nextBoolean());
            } else {
                newMap.get(deviceIds.get(i)).setConnected(false);
            }
        }
        return newMap;
    }

    public void run() {
        try {
            // Send a random list of paired devices
            HashMap<Integer, PairDevice> pairedDevices =
                    randomizeDeviceMap(mPairedDevices, true);
            Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION);
            intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, pairedDevices);
            mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);

            // Send a random list of pairing devices
            HashMap<Integer, PairDevice> pairingDevices =
                    randomizeDeviceMap(mPairingDevices, false);
            intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
            intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, pairingDevices);
            mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Exception e) {
            Log.w(TAG, "Something failed running the paired generator");
            e.printStackTrace();
        }
    }

    class DevicePairingThread implements Runnable {
        Context mAppContext;
        PairDevice mPairingDevice;
        final String TAG = "DevicePairingThread";

        public DevicePairingThread(Context appContext, PairDevice pairingDevice) {
            mAppContext = appContext;
            mPairingDevice = pairingDevice;
        }

        public void run() {
            try {
                // Send the six-digit code, waiting a couple seconds, then randomly send
                // either a success or failure message.
                String code = generateSixDigitCode();
                Log.i(TAG, "deviceName=" + mPairingDevice.getDeviceName() +
                        " code=" + code);
                Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_PAIRING_CODE);
                intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, mPairingDevice);
                intent.putExtra(AdbManager.WIRELESS_PAIRING_CODE_EXTRA, code);
                mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);

                Thread.sleep(5000);

                Random rand = new Random();
                boolean success = rand.nextBoolean();
                intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                        success ?
                            AdbManager.WIRELESS_STATUS_SUCCESS :
                            AdbManager.WIRELESS_STATUS_FAIL);
                intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, mPairingDevice);
                if (success) {
                    mPairedDevices.put(mPairingDevice.getDeviceId(),
                            mPairingDevice);
                    mPairingDevices.remove(mPairingDevice.getDeviceId());
                }
                mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } catch (InterruptedException e) {
                Log.i(TAG, "The pairing was cancelled for " + mPairingDevice.getDeviceName());
                mPairingThreads.remove(mPairingDevice.getDeviceId());
            } catch (Exception e) {
                Log.w(TAG, "Something failed while simulating device pairing");
                e.printStackTrace();
            }
            mPairingThreads.remove(mPairingDevice.getDeviceId());
        }

        String generateSixDigitCode() {
            Random rand = new Random();
            String code = "";

            for (int i = 0; i < 6; ++i) {
                // range: 1..9
                code += (rand.nextInt(9) + 1);
            }

            return code;
        }
    }
}
