/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BluetoothEventManager receives broadcasts and callbacks from the Bluetooth
 * API and dispatches the event on the UI thread to the right class in the
 * Settings.
 */
final class BluetoothEventManager {
    private static final String TAG = "BluetoothEventManager";

    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private LocalBluetoothProfileManager mProfileManager;
    private final IntentFilter mAdapterIntentFilter, mProfileIntentFilter;
    private final Map<String, Handler> mHandlerMap;
    private Context mContext;

    private final Collection<BluetoothCallback> mCallbacks =
            new ArrayList<BluetoothCallback>();

    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice device);
    }

    void addHandler(String action, Handler handler) {
        mHandlerMap.put(action, handler);
        mAdapterIntentFilter.addAction(action);
    }

    void addProfileHandler(String action, Handler handler) {
        mHandlerMap.put(action, handler);
        mProfileIntentFilter.addAction(action);
    }

    // Set profile manager after construction due to circular dependency
    void setProfileManager(LocalBluetoothProfileManager manager) {
        mProfileManager = manager;
    }

    BluetoothEventManager(LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager, Context context) {
        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mAdapterIntentFilter = new IntentFilter();
        mProfileIntentFilter = new IntentFilter();
        mHandlerMap = new HashMap<String, Handler>();
        mContext = context;

        // Bluetooth on/off broadcasts
        addHandler(BluetoothAdapter.ACTION_STATE_CHANGED, new AdapterStateChangedHandler());

        // Discovery broadcasts
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_STARTED, new ScanningStateChangedHandler(true));
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, new ScanningStateChangedHandler(false));
        addHandler(BluetoothDevice.ACTION_FOUND, new DeviceFoundHandler());
        addHandler(BluetoothDevice.ACTION_DISAPPEARED, new DeviceDisappearedHandler());
        addHandler(BluetoothDevice.ACTION_NAME_CHANGED, new NameChangedHandler());

        // Pairing broadcasts
        addHandler(BluetoothDevice.ACTION_BOND_STATE_CHANGED, new BondStateChangedHandler());
        addHandler(BluetoothDevice.ACTION_PAIRING_CANCEL, new PairingCancelHandler());

        // Fine-grained state broadcasts
        addHandler(BluetoothDevice.ACTION_CLASS_CHANGED, new ClassChangedHandler());
        addHandler(BluetoothDevice.ACTION_UUID, new UuidChangedHandler());

        // Dock event broadcasts
        addHandler(Intent.ACTION_DOCK_EVENT, new DockEventHandler());

        mContext.registerReceiver(mBroadcastReceiver, mAdapterIntentFilter);
    }

    void registerProfileIntentReceiver() {
        mContext.registerReceiver(mBroadcastReceiver, mProfileIntentFilter);
    }

    /** Register to start receiving callbacks for Bluetooth events. */
    void registerCallback(BluetoothCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    /** Unregister to stop receiving callbacks for Bluetooth events. */
    void unregisterCallback(BluetoothCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    // This can't be called from a broadcast receiver where the filter is set in the Manifest.
    private static String getDockedDeviceAddress(Context context) {
        // This works only because these broadcast intents are "sticky"
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_DOCK_EVENT));
        if (i != null) {
            int state = i.getIntExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
            if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    return device.getAddress();
                }
            }
        }
        return null;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            Handler handler = mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };

    private class AdapterStateChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                    BluetoothAdapter.ERROR);
            // update local profiles and get paired devices
            mLocalAdapter.setBluetoothStateInt(state);
            // send callback to update UI and possibly start scanning
            synchronized (mCallbacks) {
                for (BluetoothCallback callback : mCallbacks) {
                    callback.onBluetoothStateChanged(state);
                }
            }
            // Inform CachedDeviceManager that the adapter state has changed
            mDeviceManager.onBluetoothStateChanged(state);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean started) {
            mStarted = started;
        }
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            synchronized (mCallbacks) {
                for (BluetoothCallback callback : mCallbacks) {
                    callback.onScanningStateChanged(mStarted);
                }
            }
            mDeviceManager.onScanningStateChanged(mStarted);
            LocalBluetoothPreferences.persistDiscoveringTimestamp(context);
        }
    }

    private class DeviceFoundHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            // TODO Pick up UUID. They should be available for 2.1 devices.
            // Skip for now, there's a bluez problem and we are not getting uuids even for 2.1.
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                Log.d(TAG, "DeviceFoundHandler created new CachedBluetoothDevice: "
                        + cachedDevice);
                // callback to UI to create Preference for new device
                dispatchDeviceAdded(cachedDevice);
            }
            cachedDevice.setRssi(rssi);
            cachedDevice.setBtClass(btClass);
            cachedDevice.setNewName(name);
            cachedDevice.setVisible(true);
        }
    }

    private void dispatchDeviceAdded(CachedBluetoothDevice cachedDevice) {
        synchronized (mCallbacks) {
            for (BluetoothCallback callback : mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }
    }

    private class DeviceDisappearedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "received ACTION_DISAPPEARED for an unknown device: " + device);
                return;
            }
            if (CachedBluetoothDeviceManager.onDeviceDisappeared(cachedDevice)) {
                synchronized (mCallbacks) {
                    for (BluetoothCallback callback : mCallbacks) {
                        callback.onDeviceDeleted(cachedDevice);
                    }
                }
            }
        }
    }

    private class NameChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            mDeviceManager.onDeviceNameUpdated(device);
        }
    }

    private class BondStateChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            if (device == null) {
                Log.e(TAG, "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                               BluetoothDevice.ERROR);
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                /* This can happen if we get an incoming pairing request from a
                 * device which we have never found in any inquiry before.
                 * -> Add this device to the cached devices (mCachedDevices)
                 * which lists all devices around.
                 */
                Log.d(TAG, "device " + device + " not found in cached devices. Adding it");
                addNewDeviceToCache(device);

                /* Double check that it was really added: */
                cachedDevice = mDeviceManager.findDevice(device);
                if (cachedDevice == null) {
                    Log.e(TAG, "Got bonding state changed for " + device +
                            ", but device could not be added to cache.");
                    return;
                }
            }

            synchronized (mCallbacks) {
                for (BluetoothCallback callback : mCallbacks) {
                    callback.onDeviceBondStateChanged(cachedDevice, bondState);
                }
            }
            cachedDevice.onBondingStateChanged(bondState);

            if (bondState == BluetoothDevice.BOND_NONE) {
                if (device.isBluetoothDock()) {
                    // After a dock is unpaired, we will forget the settings
                    LocalBluetoothPreferences
                            .removeDockAutoConnectSetting(context, device.getAddress());

                    // if the device is undocked, remove it from the list as well
                    if (!device.getAddress().equals(getDockedDeviceAddress(context))) {
                        cachedDevice.setVisible(false);
                    }
                }
                int reason = intent.getIntExtra(BluetoothDevice.EXTRA_REASON,
                        BluetoothDevice.ERROR);

                showUnbondMessage(context, cachedDevice.getName(), reason);
            }
        }

        /**
         * Called when we have reached the unbonded state.
         *
         * @param reason one of the error reasons from
         *            BluetoothDevice.UNBOND_REASON_*
         */
        private void showUnbondMessage(Context context, String name, int reason) {
            int errorMsg;

            switch(reason) {
            case BluetoothDevice.UNBOND_REASON_AUTH_FAILED:
                errorMsg = R.string.bluetooth_pairing_pin_error_message;
                break;
            case BluetoothDevice.UNBOND_REASON_AUTH_REJECTED:
                errorMsg = R.string.bluetooth_pairing_rejected_error_message;
                break;
            case BluetoothDevice.UNBOND_REASON_REMOTE_DEVICE_DOWN:
                errorMsg = R.string.bluetooth_pairing_device_down_error_message;
                break;
            case BluetoothDevice.UNBOND_REASON_DISCOVERY_IN_PROGRESS:
            case BluetoothDevice.UNBOND_REASON_AUTH_TIMEOUT:
            case BluetoothDevice.UNBOND_REASON_REPEATED_ATTEMPTS:
            case BluetoothDevice.UNBOND_REASON_REMOTE_AUTH_CANCELED:
                errorMsg = R.string.bluetooth_pairing_error_message;
                break;
            default:
                Log.w(TAG, "showUnbondMessage: Not displaying any message for reason: " + reason);
                return;
            }
            Utils.showError(context, name, errorMsg);
        }
    }

    private class ClassChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            mDeviceManager.onBtClassChanged(device);
        }
    }

    private class UuidChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            mDeviceManager.onUuidChanged(device);
        }
    }

    private class PairingCancelHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.e(TAG, "ACTION_PAIRING_CANCEL with no EXTRA_DEVICE");
                return;
            }
            int errorMsg = R.string.bluetooth_pairing_error_message;
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            Utils.showError(context, cachedDevice.getName(), errorMsg);
        }
    }

    private class DockEventHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            // Remove if unpair device upon undocking
            int anythingButUnDocked = Intent.EXTRA_DOCK_STATE_UNDOCKED + 1;
            int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, anythingButUnDocked);
            if (state == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                if (device != null && device.getBondState() == BluetoothDevice.BOND_NONE) {
                    CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
                    if (cachedDevice != null) {
                        cachedDevice.setVisible(false);
                    }
                }
            }
        }
    }

    void addNewDeviceToCache(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = mDeviceManager.addDevice(mLocalAdapter,
                mProfileManager, device);
        Log.d(TAG, "addNewDeviceToCache: add new device to mCachedDevices: address = "
                + cachedDevice.getDevice().toString() + " name = " + cachedDevice.getName());
        dispatchDeviceAdded(cachedDevice);
    }

    boolean readPairedDevices() {
        Set<BluetoothDevice> bondedDevices = mLocalAdapter.getBondedDevices();
        if (bondedDevices == null) {
            Log.e(TAG, "readPairedDevices: an error occurs. abort.");
            return false;
        }
        if (bondedDevices.size() == 0) {
            Log.d(TAG, "readPairedDevices: there is no bonded device");
        }

        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            Log.d(TAG, "readPairedDevices: bondedDevice info: address = " + device.toString()
                    + " name = " + device.getName() + " type = " + device.getType());
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                Log.d(TAG, "readPairedDevices: add bonded device to mCachedDevices: address = "
                        + cachedDevice.getDevice().toString() + " name = " + cachedDevice.getName());
                dispatchDeviceAdded(cachedDevice);
                deviceAdded = true;
            }
        }

        return deviceAdded;
    }
}
