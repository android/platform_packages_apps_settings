/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.wimax;

import static android.net.wimax.WimaxManager.WIMAX_DISABLED;
import static android.net.wimax.WimaxManager.WIMAX_ENABLED;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wimax.WimaxState;
import android.net.wimax.WimaxManager;
import android.net.wimax.structs.ConnectedNspInfo;
import android.net.wimax.structs.NSPInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Config;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for abstracting Wimax.
 * <p>
 * Client must call {@link #onCreate()}, {@link #onCreatedCallback()},
 * {@link #onPause()}, {@link #onResume()}.
 */
public class WimaxLayer {

    private static final String TAG = "SettingsWimaxLayer";
    static final boolean LOGV = false || Config.LOGV;

    //============================
    // Other member variables
    //============================

    private Context mContext;
    private Callback mCallback;

    static final int MESSAGE_ATTEMPT_SCAN = 1;
    private Handler mHandler = new MyHandler();

    //============================
    // Wimax member variables
    //============================

    private WimaxManager mWimaxManager;
    private IntentFilter mIntentFilter;
    private List<NSPInfo> mCurrentNetworkList;
    private String mCurrentNspName;
    private String mNspToConnect;

    /** The delay between scans when we're continually scanning. */
    private static final int CONTINUOUS_SCAN_DELAY_MS = 6000;
    /** On failure, the maximum retries for scanning. */
    private static final int SCAN_MAX_RETRY = 5;
    /** On failure, the delay between each scan retry. */
    private static final int SCAN_RETRY_DELAY_MS = 1000;
    /** On failure, the number of retries so far. */
    private int mScanRetryCount = 0;
    /**
     * Whether we're currently obtaining an address. Continuous scanning will be
     * disabled in this state.
     */
    private boolean mIsObtainingAddress = false;

    /**
     * The current wimax state, as broadcasted.
     */
    private WimaxState mCurrentWimaxState = WimaxState.UNKNOWN;

    //============================
    // Inner classes
    //============================

    interface Callback {
        void onError(int messageResId);

        /**
         * Called when a wimax network is added or removed.
         *
         * @param nspName name of the network.
         * @param added {@code true} if added, {@code false} if removed.
         */
        void onNetworkListChanged(NSPInfo nspInfo, boolean added);

        /**
         * Called when the scanning status changes.
         *
         * @param started {@code true} if the scanning just started,
         *            {@code false} if it just ended.
         */
        void onScanningStatusChanged(boolean started);

        /**
         * Called when either connected to/disconnected from a network or Wimax is
         * being enabled/disabled.
         *
         * @param enabled {@code true} should be enabled, {@code false}
         *            should be disabled.
         */
        void onWimaxStatusChanged(boolean enabled);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WimaxManager.WIMAX_STATE_CHANGED_ACTION)) {
                handleWimaxStateChanged(
                        (WimaxState) intent.getParcelableExtra(WimaxManager.EXTRA_WIMAX_STATE),
                        intent.getStringExtra(WimaxManager.EXTRA_BSID));
            } else if (action.equals(WimaxManager.WIDE_SCAN_RESULTS_AVAILABLE_ACTION)) {
                NSPInfo[] networkList = (NSPInfo[])intent.getParcelableArrayExtra(WimaxManager.EXTRA_SCAN_RESULTS);
                if (LOGV) {
                    Log.v(TAG, "Network search wide scan results received. " + networkList);
                }
                List<NSPInfo> nspList = new ArrayList<NSPInfo>();
                for(int i=0; i<networkList.length; i++) {
                    nspList.add(networkList[i]);
                }
                handleScanResultsAvailable(nspList);

            } else if (action.equals(WimaxManager.WIMAX_STATUS_CHANGED_ACTION)) {
                handleWimaxStatusChanged(intent.getIntExtra(WimaxManager.EXTRA_WIMAX_STATUS,
                        WimaxManager.WIMAX_STATUS_UNKNOWN));
            } else if (action.equals(WimaxManager.RSSI_CHANGED_ACTION)) {
                handleSignalStrengthChanged(intent.getIntExtra(WimaxManager.EXTRA_NEW_RSSI, 0));
            }
        }
    };

    /**
     * If using this class, make sure to call the callbacks of this class, such
     * as {@link #onCreate()}, {@link #onCreatedCallback()},
     * {@link #onPause()}, {@link #onResume()}.
     *
     * @param context The context.
     * @param callback The interface that will be invoked when events from this
     *            class are generated.
     */
    public WimaxLayer(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
    }

    //============================
    // Lifecycle
    //============================

    /**
     * The client MUST call this.
     * <p>
     * This shouldn't have any dependency on the callback.
     */
    public void onCreate() {
        mWimaxManager = (WimaxManager) mContext.getSystemService(Context.WIMAX_SERVICE);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WimaxManager.WIDE_SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WimaxManager.WIMAX_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WimaxManager.WIMAX_STATUS_CHANGED_ACTION);
        mIntentFilter.addAction(WimaxManager.RSSI_CHANGED_ACTION);
    }

    /**
     * The client MUST call this.
     * <p>
     * Callback is ready.
     */
    public void onCreatedCallback() {
        if (isWimaxEnabled()) {
            mCurrentWimaxState = mWimaxManager.getWimaxState();
            if (mCurrentWimaxState.isConnected()) {
                ConnectedNspInfo connectedNspInfo = mWimaxManager.getConnectedNSP();
                mCurrentNspName = connectedNspInfo.getName();
            }
            attemptScan();
        }
    }

    /**
     * The client MUST call this.
     *
     * @see android.app.Activity#onResume
     */
    public void onResume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);

        if (isWimaxEnabled()) {
            // Kick start the continual scan
            queueContinuousScan();
        }
    }

    /**
     * The client MUST call this.
     *
     * @see android.app.Activity#onPause
     */
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);

        removeFutureScans();
    }

    //============================
    // "Public" API
    //============================

    /**
     * Returns the name of currently connected NSP.
     *
     * @return String - name of connected nsp.
     */
    public String getCurrentNspName() {
        return mCurrentNspName;
    }

    /**
     * Returns the name of NSP to which WimaxManager is trying to connect.
     *
     * @return String - name of connecting nsp.
     */
    public String getNspToConnect() {
        return mNspToConnect;
    }

    /**
     * Connects to the specified network.
     *
     * @param nspName Name of the nsp.
     * @return Whether the operation was successful.
     */
    public boolean connectToNetwork(String nspName) {
        if (LOGV) {
            Log.v(TAG, "Connecting to " + nspName);
        }
        mNspToConnect = nspName;

        removeFutureScans();
        mWimaxManager.connect(nspName);

        return true;
    }

    /**
     * Disconnects from the currently connected network.
     *
     * @return Whether the operation was successful.
     */
    public boolean disconnectFromNetwork() {
        if (LOGV) {
            Log.v(TAG, "Disconnecting from " + mCurrentNspName);
        }

        if (mCurrentWimaxState.isConnected()) {
            removeFutureScans();
            mWimaxManager.disconnect();
        }

        return true;
    }

    /**
     * Attempts to scan networks.  This has a retry mechanism.
     */
    public void attemptScan() {

        // Remove any future scans since we're scanning right now
        removeFutureScans();

        if (!mWimaxManager.isWimaxEnabled()) return;

        List<NSPInfo> networkList = mWimaxManager.getNetworkList();
        if (networkList != null) {
            postAttemptScan(networkList);
        } else {
            mScanRetryCount = 0;
        }
    }

    private void queueContinuousScan() {
        removeFutureScans();

        if (!mIsObtainingAddress) {
            // Don't do continuous scan while in obtaining IP state
            mHandler.sendEmptyMessageDelayed(MESSAGE_ATTEMPT_SCAN, CONTINUOUS_SCAN_DELAY_MS);
        }
    }

    /**
     * Attempt the network wide scan.
     */
    public void attemptWideScan() {
        removeFutureScans();

        if (!mWimaxManager.isWimaxEnabled()) return;

        if(!mWimaxManager.performWideScan()) {
            error(R.string.error_wimax_scanning);
            onScanningEnded();
        }
    }

    private void removeFutureScans() {
        mHandler.removeMessages(MESSAGE_ATTEMPT_SCAN);
    }

    public boolean isWimaxEnabled() {
        return mWimaxManager.isWimaxEnabled();
    }

    public void error(int messageResId) {
        Log.e(TAG, mContext.getResources().getString(messageResId));

        if (mCallback != null) {
            mCallback.onError(messageResId);
        }
    }

    private void postAttemptScan(List<NSPInfo> networkList) {
        if(mScanRetryCount == 0)
            onScanningStarted();

        if(networkList.size() > 0) {
            handleScanResultsAvailable(networkList);
        }else {
            if (++mScanRetryCount < SCAN_MAX_RETRY) {
                // Just in case, remove previous ones first
                removeFutureScans();
                mHandler.sendEmptyMessageDelayed(MESSAGE_ATTEMPT_SCAN, SCAN_RETRY_DELAY_MS);
            } else {
                // Show an error once we run out of attempts
                error(R.string.error_wimax_scanning);
                onScanningEnded();
            }
        }
    }

    private void onScanningStarted() {
        if (mCallback != null) {
            mCallback.onScanningStatusChanged(true);
        }
    }

    private void onScanningEnded() {
        queueContinuousScan();

        if (mCallback != null) {
            mCallback.onScanningStatusChanged(false);
        }
    }

    private void clearNetworkList() {
        if(mCurrentNetworkList != null) {
            List<NSPInfo> networkList = new ArrayList<NSPInfo>();

            synchronized(this) {
                // Clear the logic's list of access points
                networkList.addAll(mCurrentNetworkList);
                mCurrentNetworkList.clear();
            }

            for (int i = networkList.size() - 1; i >= 0; i--) {
                removeNSPFromUI(networkList.get(i));
            }
        }
    }

    private void removeNSPFromUI(NSPInfo nspInfo) {
        if (mCallback != null) {
            mCallback.onNetworkListChanged(nspInfo, false);
        }
    }


    //============================
    // Wimax callbacks
    //============================

    private void handleScanResultsAvailable(List<NSPInfo> networkList) {
        synchronized(this) {
            clearNetworkList();

            for(int i=0; i<networkList.size(); i++) {
                NSPInfo nspInfo = (NSPInfo)networkList.get(i);
                if (mCallback != null) {
                    mCallback.onNetworkListChanged(nspInfo, true);
                }
            }
            mCurrentNetworkList = networkList;
        }

        onScanningEnded();

        if(!mCurrentWimaxState.isConnected() && Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.WIMAX_AUTO_CONNECT_ON, 0) == 1 ) {
            String lastConnectedNspName = Settings.Secure.getString(mContext.getContentResolver(),
                    Settings.Secure.WIMAX_LAST_CONNECTED_NETWORK);
            for(int i=0; i<networkList.size(); i++) {
                NSPInfo nspInfo = (NSPInfo)networkList.get(i);
                if(lastConnectedNspName != null && lastConnectedNspName.equalsIgnoreCase(nspInfo.getNspName())) {
                    connectToNetwork(nspInfo.getNspName());
                    break;
                }
            }
        }
    }

    private void handleWimaxStatusChanged(int wimaxStatus) {

        if (wimaxStatus == WIMAX_ENABLED) {
            attemptScan();

        } else if (wimaxStatus == WIMAX_DISABLED) {
            removeFutureScans();
            if (LOGV) Log.v(TAG, "Clearing Network list because wimax is disabled");
            clearNetworkList();
        }

        if (mCallback != null) {
            mCallback.onWimaxStatusChanged(wimaxStatus == WIMAX_ENABLED);
        }
    }

    private void handleSignalStrengthChanged(int rssi) {

    }

    private void handleWimaxStateChanged(WimaxState state, String bsid) {
        mCurrentWimaxState = state;
        if (mCallback != null) {
            mCallback.onWimaxStatusChanged(state.isConnected());
        }

        if (state.isConnected()) {
            ConnectedNspInfo connectedNspInfo = mWimaxManager.getConnectedNSP();
            mCurrentNspName = connectedNspInfo.getName();
            mNspToConnect = "";
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.WIMAX_LAST_CONNECTED_NETWORK, mCurrentNspName);
            queueContinuousScan();
        }else if(state == WimaxState.INITIALIZED) {
            queueContinuousScan();
            mCurrentNspName = "";
        }else if(state != WimaxState.DISCONNECTING){
            mCurrentNspName = "";
        }
    }

    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ATTEMPT_SCAN:
                    attemptScan();
                    break;
            }
        }
    }

}
