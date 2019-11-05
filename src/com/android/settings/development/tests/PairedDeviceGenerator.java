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

import java.lang.Runnable;

import java.util.ArrayList;
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

    // the initial list of devices currently paired with.
    private static HashMap<String, PairDevice> mPairedDevices = createPairedDevices();

    // the initial list of devices ready to be paired with.
    private static HashMap<String, PairDevice> mPairingDevices = createPairingDevices();

    private static final String mQrCodeString = "http://goo.gl/TkGr5s";
    private static final PairDevice mQrCodeDevice =
            new PairDevice("Batman's QRCode Device", "77:2F:71:11:F1:FF", false);

    private static HashMap<String, PairDevice> createPairedDevices() {
        HashMap<String, PairDevice> map = new HashMap<String, PairDevice>();
        map.put("32:07:43:05:2B:05", new PairDevice("Josh's Macbook", "32:07:43:05:2B:05", false));
        map.put("89:D2:77:50:76:E7", new PairDevice("John's Chromebook", "89:D2:77:50:76:E7", false));
        map.put("04:6C:19:9C:E0:F1", new PairDevice("Bob's Windows", "04:6C:19:9C:E0:F1", false));
        map.put("0E:3F:78:53:00:96", new PairDevice("Mary's mainframe", "0E:3F:78:53:00:96", false));
        map.put("F5:C8:96:50:8D:99", new PairDevice("Sam's Ubuntu", "F5:C8:96:50:8D:99", false));
        return map;
    }

    private static HashMap<String, PairDevice> createPairingDevices() {
        HashMap<String, PairDevice> map = new HashMap<String, PairDevice>();
        map.put("32:07:55:05:2B:05", new PairDevice("Jane's Macbook Pro", "32:07:55:05:2B:05", false));
        map.put("89:D2:89:50:76:E7", new PairDevice("Jim's iMac", "89:D2:89:50:76:E7", false));
        map.put("04:6C:6A:9C:E0:F1", new PairDevice("Suzie's Windows", "04:6C:6A:9C:E0:F1", false));
        map.put("0E:3F:12:53:00:96", new PairDevice("Al's Ubuntu", "0E:3F:12:53:00:96", false));
        map.put("F5:C8:9F:50:8D:99", new PairDevice("Jill's unknown", "F5:C8:9F:50:8D:99", false));
        return map;
    }

    // Thread running the qrcode/six-digit discoverable thread
    private ScheduledFuture mDiscoverableThread;
    private ScheduledFuture mQrcodePairingThread;

    private HashMap<String, PairDevice> mPairedDeviceMap;
    private HashMap<String, PairDevice> mPairingDeviceMap;

    public PairedDeviceGenerator(Context appContext) {
        mAppContext = appContext;
        mScheduledTaskExecutor = Executors.newScheduledThreadPool(1);
    }

    // force grab the device list. Will send the action event.
    public void requestPairedList() {
        Intent intent = new Intent(WirelessDebuggingManager.WIRELESS_DEBUG_PAIRED_LIST_ACTION);
        intent.putExtra(WirelessDebuggingManager.DEVICE_LIST_EXTRA, mPairedDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    // force grab the pairing device list. Will send the action event.
    public void queryAdbWirelessPairingDevices() {
        Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
        intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, mPairingDeviceMap);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void pairDevice(Integer mode, String guid, String qrcode) {
        mQrcodePairingThread = mScheduledTaskExecutor.schedule(
                new QrcodePairingThread(mAppContext, qrcode),
                    1,
                    TimeUnit.SECONDS);
    }

    public void unpairDevice(String guid) {
        // Move it into the pairing devices hash map.
        mPairingDevices.put(guid, mPairedDevices.get(guid));
        mPairedDevices.remove(guid);
    }

    public void cancelPairing(Integer mode, String guid) {
        if (mDiscoverableThread != null && !mDiscoverableThread.isDone()) {
            mDiscoverableThread.cancel(true);
        }
        if (mQrcodePairingThread != null && !mDiscoverableThread.isDone()) {
            mQrcodePairingThread.cancel(true);
        }
    }

    public void setDiscoverable(int mode, boolean enable) {
        if (!enable) {
            cancelPairing(-1, "");
            return;
        }

        mDiscoverableThread = mScheduledTaskExecutor.schedule(
                new DiscoverableSimulator(mAppContext, mode),
                    1,
                    TimeUnit.SECONDS);
    }

    public void start() {
        if (mStarted) {
            return;
        }

        Log.i(TAG, "Starting PairedDeviceGenerator");
        // This schedules a task to run every 10 seconds
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
    private HashMap<String, PairDevice> randomizeDeviceMap(HashMap<String, PairDevice> map, boolean randomizeConnected) {
        HashMap<String, PairDevice> newMap = new HashMap<String, PairDevice>();

        Log.i(TAG, "randomizeDeviceMap() device list: " + mPairedDevices);
        Random rand = new Random();
        // Get a random subset of mPairedDevices
        int numDevices = rand.nextInt(mPairedDevices.size() + 1);

        if (numDevices == 0) {
            return;
        }

        // Grab randomly numDevices from map
        List<String> deviceGuids = new ArrayList<String>(map.keySet());
        Collections.shuffle(deviceGuids);

        for (int i = 0; i < numDevices; ++i) {
            newMap.put(deviceGuids.get(i), map.get(deviceGuids.get(i)));
            if (randomizeConnected) {
            // Randomly choose whether the device is connected or not
                newMap.get(deviceGuids.get(i)).setConnected(
                    rand.nextBoolean());
            } else {
                newMap.get(deviceGuids.get(i)).setConnected(false);
            }
        }
    }

    public void run() {
        try {
            // Send a random list of paired devices
            HashMap<String, PairDevice> pairedDevices =
                    randomizeDeviceMap(mPairedDevices, true);
            Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION);
            intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, pairedDevices);
            mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);

            // Send a random list of pairing devices
            HashMap<String, PairDevice> pairingDevices =
                    randomizeDeviceMap(mPairingDevices, false);
            intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
            intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, pairingDevices);
            mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Exception e) {
            Log.w(TAG, "Something failed running the paired generator");
            e.printStackTrace();
        }

    public void requestFakeQrcodeResult(Activity activity, QrCamera.ScannerCallback cb) {
        ScheduledFuture future = mScheduledTaskExecutor.schedule(
                new QrCodeGeneratorThread(mAppContext, activity, cb),
                    1,
                    TimeUnit.SECONDS);
    }

    class QrCodeGeneratorThread implements Runnable {
        Context mAppContext;
        Activity mActivity;
        final String TAG = "QrCodeGeneratorThread";
        QrCamera.ScannerCallback mCallback;

        public QrCodeGeneratorThread(Context appContext, Activity activity, QrCamera.ScannerCallback cb) {
            mAppContext = appContext;
            mActivity = activity;
            mCallback = cb;
        }

        public void run() {
            try {
                Thread.sleep(3000);
                // Randomly return a good/bad qrcode result
                Random rand = new Random();
                boolean success = rand.nextBoolean();
                String res = success ? mQrCodeString : "notTheCorrectQrCode";
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.handleSuccessfulResult(res);
                    }
                });
            } catch (InterruptedException e) {
                Log.i(TAG, "The qrcode simulation was cancelled");
            } catch (Exception e) {
                Log.w(TAG, "Something failed while doing qrcode simulation");
                e.printStackTrace();
            }
        }
    }

    class DiscoverableSimulator implements Runnable {
        // Represents the user flow from when one opens either the "pairing by qr code" or
        // "pairing by six-digit code".
        Context mAppContext;
        final String TAG = "DiscoverableSimulator";
        // Pairing via QR code or six-digit code.
        int mMode;

        public DiscoverableSimulator(Context appContext, int mode) {
            mAppContext = appContext;
            mMode = mode;
        }

        public void run() {
            try {
                if (mMode == AdbManager.WIRELESS_DEBUG_PAIR_MODE_QR) {
                    Log.i(TAG, "Starting DiscoverableSimulator for QrCode");
                    // only need to send discoverable result
                    Thread.sleep(2000);
                    Random rand = new Random();
                    Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_ENABLE_DISCOVER_ACTION);
                    boolean success = rand.nextBoolean();
                    intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                            success ? AdbManager.WIRELESS_STATUS_SUCCESS :
                                      AdbManager.WIRELESS_STATUS_FAIL);
                    mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                } else if (mMode == AdbManager.WIRELESS_DEBUG_PAIR_MODE_CODE) {
                    Log.i(TAG, "Starting DiscoverableSimulator for six-digit pairing");
                    // need to send discoverable result + pairing code + pairing result
                    Thread.sleep(2000);
                    Random rand = new Random();
                    Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_ENABLE_DISCOVER_ACTION);
                    boolean discoverSuccess = rand.nextBoolean();
                    // send discoverable result
                    intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                            discoverSuccess ? AdbManager.WIRELESS_STATUS_SUCCESS :
                                      AdbManager.WIRELESS_STATUS_FAIL);
                    mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    // only proceed if discoverable was enabled
                    if (!discoverSuccess) {
                        return;
                    }

                    // next, send pairing code
                    Thread.sleep(3000);
                    String code = generateSixDigitCode();
                    intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                    intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                            AdbManager.WIRELESS_STATUS_PAIRING_CODE);
                    intent.putExtra(AdbManager.WIRELESS_PAIRING_CODE_EXTRA, code);
                    mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);

                    // next, simulate pairing success/failure
                    Thread.sleep(5000);
                    boolean pairSuccess = rand.nextBoolean();
                    intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                    intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                            pairSuccess ?
                                AdbManager.WIRELESS_STATUS_SUCCESS :
                                AdbManager.WIRELESS_STATUS_FAIL);
                    if (pairSuccess) {
                        // Randomly select a device and put into the paired pool
                        List<String> deviceGuids = new ArrayList<String>(mPairingDevices.keySet());
                        Collections.shuffle(deviceGuids);
                        mPairedDevices.put(deviceGuids.get(0),
                                mPairingDevices.get(deviceGuids.get(0)));
                        mPairingDevices.remove(deviceGuids.get(0));
                    }

                    mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "The pairing was cancelled");
            } catch (Exception e) {
                Log.w(TAG, "Something failed while simulating device discoverability");
            }
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

    /**
     * Thread to just check to see if the qr code matches mQrCodeString.
     */
    class QrcodePairingThread implements Runnable {
        Context mAppContext;
        final String TAG = "QrcodePairingThread";
        String mQrCode;

        public QrcodePairingThread(Context appContext, String qrCode) {
            mAppContext = appContext;
            mQrCode = qrCode;
        }

        public void run() {
            try {
                // Pairing via QR code
                Thread.sleep(3000);
                Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                boolean success = mQrCode.equals(mQrCodeString);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                        success ?
                            AdbManager.WIRELESS_STATUS_SUCCESS :
                            AdbManager.WIRELESS_STATUS_FAIL);
                if (success) {
                    // Randomly select a device and put into the paired pool
                    List<String> deviceGuids = new ArrayList<String>(mPairingDevices.keySet());
                    Collections.shuffle(deviceGuids);
                    mPairedDevices.put(deviceGuids.get(0),
                            mPairingDevices.get(deviceGuids.get(0)));
                    mPairingDevices.remove(deviceGuids.get(0));
                }
                mAppContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } catch (InterruptedException e) {
                Log.i(TAG, "The qr pairing was cancelled");
            } catch (Exception e) {
                Log.w(TAG, "Something failed while simulating device pairing");
                e.printStackTrace();
            }
        }
    }
}

